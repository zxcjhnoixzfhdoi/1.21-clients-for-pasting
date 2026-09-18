package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventMove;
import hack.echo.client.event.impl.EventOnAttackEntity;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.api.AttackRangeCompat;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.rotation.RotationUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import static net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK;

public class KnockbackDisplaceModule extends Feature {

    private static final CharSequence LEFT = Concat.of("Left");
    private static final CharSequence RIGHT = Concat.of("Right");
    private static final CharSequence BACK = Concat.of("Back");
    private static final CharSequence SPACE = Concat.of(" ");

    private static final int ATTACK_TIMEOUT_TICKS = 1;

    private final BoolSetting supportKnockbackEnchant = new BoolSetting(Concat.of("Knockback Enchant"), true);
    private final ModeSetting direction = new ModeSetting(Concat.of("Direction"), BACK, LEFT, RIGHT, BACK);
    private final FloatSetting yawOffset = new FloatSetting(Concat.of("Yaw Offset"), 90.0f, 0.0f, 180.0f, 5.0f, o -> !direction.is(BACK));

    private boolean hadSilentRotation;
    private boolean waitingForSpoofedMovement;
    private boolean readyToReplayAttack;
    private boolean releasingSpoofRotation;
    private boolean replayingAttack;

    private int pendingTargetId = -1;
    private int timeoutTicks;

    private float queuedYaw;
    private float queuedPitch;
    private float previousServerYaw;
    private float previousServerPitch;

    public KnockbackDisplaceModule() {
        super(new FeatureInfo(
            Concat.of("Knockback Displace"),
            Concat.of("Delays knockback hits by one tick to redirect knockback"),
            Category.COMBAT
        ));
    }

    @Override
    public CharSequence concat() {
        if (direction.is(BACK)) {
            return BACK;
        }

        CharSequence offset = Concat.ofInt(Math.round(yawOffset.getValue()));
        if (direction.is(LEFT)) {
            return Concat.of(LEFT, SPACE, offset);
        }

        return Concat.of(RIGHT, SPACE, offset);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGHEST)
    private void onStartAttack(EventStartAttack event) {
        if (!shouldStageAttack(event)) {
            return;
        }

        queuedYaw = getQueuedYaw(mc.player.getYRot());
        queuedPitch = mc.player.getXRot();
        pendingTargetId = event.getTarget().getId();
        timeoutTicks = ATTACK_TIMEOUT_TICKS;
        hadSilentRotation = RotationHandler.isHasSilentRotation();
        previousServerYaw = RotationHandler.getServerYaw();
        previousServerPitch = RotationHandler.getServerPitch();

        RotationUtils.forceSilentRotations(queuedYaw, queuedPitch);
        waitingForSpoofedMovement = true;
        readyToReplayAttack = false;
        replayingAttack = false;

        event.cancel();
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGHEST)
    private void onMove(EventMove.Pre event) {
        if (!waitingForSpoofedMovement && !releasingSpoofRotation) return;

        event.setYaw(RotationUtils.getSilentYaw());
        event.setPitch(RotationUtils.getSilentPitch());
    }

    @EventSubscribe(priority = EventSubscribe.Priority.LOWEST)
    private void onMovePost(EventMove.Post event) {
        if (waitingForSpoofedMovement) {
            waitingForSpoofedMovement = false;
            readyToReplayAttack = true;
            return;
        }

        if (!releasingSpoofRotation) return;

        finishRelease();
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGHEST)
    private void onHandleInputEarly(EventHandleInput.Early event) {
        if (!readyToReplayAttack) return;

        timeoutTicks--;
        if (timeoutTicks < 0) {
            resetState();
            return;
        }

        Entity target = getPendingTarget();
        if (!canReplayAttack(target)) {
            resetState();
            return;
        }

        readyToReplayAttack = false;
        replayingAttack = true;
        mc.gameMode.attack(mc.player, target);
        if (!mc.player.isSpectator()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @EventSubscribe(priority = EventSubscribe.Priority.HIGHEST)
    private void onAttackEntityPost(EventOnAttackEntity.Post event) {
        if (!replayingAttack) return;
        if (mc.player == null || event.getPlayer() != mc.player) return;
        if (!isPendingTarget(event.getTarget())) return;

        if (hadSilentRotation) {
            restorePreviousSilentRotation();
            return;
        }

        beginReleaseToPlayerRotation();
    }

    private boolean shouldStageAttack(EventStartAttack event) {
        if (isNull()) return false;
        if (event.cancelled) return false;
        if (waitingForSpoofedMovement) return false;
        if (readyToReplayAttack) return false;
        if (releasingSpoofRotation) return false;
        if (replayingAttack) return false;
        if (event.getPlayer() != mc.player) return false;
        if (!(event.getTarget() instanceof LivingEntity target)) return false;
        if (!TargetUtils.isTargetAllowed(target)) return false;
        if (mc.player.getAttackStrengthScale(0.5f) <= 0.9f) return false;
        if (target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0) return false;
        if (target.isBlocking()) return false;
        if (!hasExtraKnockback()) return false;
        if (!canReplayAttack(target)) return false;
        return true;
    }

    private boolean isPendingTarget(Entity target) {
        if (target == null) return false;
        return target.getId() == pendingTargetId;
    }

    private Entity getPendingTarget() {
        if (mc.level == null) return null;
        return mc.level.getEntity(pendingTargetId);
    }

    private boolean canReplayAttack(Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) return false;
        if (mc.player == null) return false;
        if (mc.gameMode == null) return false;
        if (!TargetUtils.isTargetAllowed(livingTarget)) return false;

        ItemStack weapon = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!weapon.isItemEnabled(mc.level.enabledFeatures())) return false;
        if (mc.player.isHandsBusy()) return false;
        if (mc.player.cannotAttackWithItem(weapon, 0)) return false;
        return AttackRangeCompat.isTargetInRange(mc.player, livingTarget);
    }

    private boolean hasExtraKnockback() {
        if (mc.player.isSprinting()) return true;

        if (!supportKnockbackEnchant.getValue()) return false;

        ItemStack weapon = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        var knockbackHolder = mc.level.registryAccess().get(KNOCKBACK);
        if (knockbackHolder.isEmpty()) {
            return false;
        }

        return EnchantmentHelper.getItemEnchantmentLevel(knockbackHolder.get(), weapon) > 0;
    }

    private float getQueuedYaw(float baseYaw) {
        if (direction.is(LEFT)) {
            return Mth.wrapDegrees(baseYaw - yawOffset.getValue());
        }

        if (direction.is(RIGHT)) {
            return Mth.wrapDegrees(baseYaw + yawOffset.getValue());
        }

        return Mth.wrapDegrees(baseYaw + 180.0f);
    }

    private void restorePreviousSilentRotation() {
        RotationUtils.forceSilentRotations(previousServerYaw, previousServerPitch);
        clearState();
    }

    private void beginReleaseToPlayerRotation() {
        if (mc.player == null) {
            RotationHandler.reset();
            clearState();
            return;
        }

        RotationUtils.forceSilentRotations(mc.player.getYRot(), mc.player.getXRot());
        releasingSpoofRotation = true;
        waitingForSpoofedMovement = false;
        readyToReplayAttack = false;
        replayingAttack = false;
        pendingTargetId = -1;
        timeoutTicks = 0;
    }

    private void finishRelease() {
        if (mc.player == null) {
            RotationHandler.reset();
            clearState();
            return;
        }

        RotationUtils.stopTracking();
        clearState();
    }

    private void resetState() {
        if (mc.player != null) {
            if (hadSilentRotation) {
                RotationUtils.forceSilentRotations(previousServerYaw, previousServerPitch);
            } else {
                RotationUtils.stopTracking();
            }
        } else {
            RotationHandler.reset();
        }

        clearState();
    }

    private void clearState() {
        hadSilentRotation = false;
        waitingForSpoofedMovement = false;
        readyToReplayAttack = false;
        releasingSpoofRotation = false;
        replayingAttack = false;
        pendingTargetId = -1;
        timeoutTicks = 0;
    }
}
