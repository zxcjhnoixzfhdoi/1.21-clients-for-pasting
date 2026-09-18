package hack.echo.client.features.impl.combat;

import hack.echo.client.Echo;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.*;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.impl.HurtTickHandler;
import hack.echo.client.utils.combat.CombatUtils;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.player.PlayerIntersectionUtil;
import hack.echo.client.utils.player.PlayerUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TriggerBot extends Feature {
    private static final CharSequence EMPTY = Concat.of();

    private final ModeSetting critMode = new ModeSetting(
            Concat.of("Crit Mode"),
            Concat.of("pCrit"),
            Concat.of("pCrit"),
            Concat.of("normal"),
            Concat.of("None")
    );
    private final BoolSetting requireSpaceToCrit = new BoolSetting(Concat.of("Crit only on space"), false);
    private final BoolSetting attackShieldedTargets = new BoolSetting(Concat.of("Attack Shields"), true);
    private final BoolSetting hurtTimeCheck = new BoolSetting(Concat.of("HurtTime Check"), true);
    private final ItemPickerSetting itemWhitelist = new ItemPickerSetting(Concat.of("Item Whitelist"));
    private final RangeSetting attackCooldown = new RangeSetting(Concat.of("Cooldown"), 90, 100, 0, 100, 1);
    private final RangeSetting critCooldown = new RangeSetting(Concat.of("Crit Cooldown"), 70, 89, 0, 100, 1);
    private final RangeSetting allowedCritRange = new RangeSetting(Concat.of("Crit Range"), 1.0f, 2.2f, 0f, 6f, 0.1f);
    private final IntSetting reactionDelay = new IntSetting(Concat.of("Reaction Delay"), 0, 0, 500);
    private final IntSetting attackDelay = new IntSetting(Concat.of("Attack Delay"), 0, 0, 500);
    private final BoolSetting requireClick = new BoolSetting(Concat.of("Require Attack Key"), false);
    private final BoolSetting unshield = new BoolSetting(Concat.of("Unshield"), true);
    private final BoolSetting clickSimulation = new BoolSetting(Concat.of("Click Simulation"), false);

    private final TimerUtils reactionTimer = new TimerUtils();
    private final TimerUtils attackTimer = new TimerUtils();
    private final TimerUtils pSync = new TimerUtils();
    
    private boolean unblocked;
    private boolean shouldReblockShield = false;
    private boolean hasValidTarget = false;
    private boolean shouldAttack = false;
    
    private long lastAttackTick = -1;
    private LivingEntity targetToAttack = null;

    public TriggerBot() {
        super(new FeatureInfo(
                Concat.of("Trigger Bot"),
                Concat.of("Automatically attacks when aiming at targets"),
                Category.COMBAT
        ));
    }

    @Override
    public CharSequence concat() {
        if (critMode.is(Concat.of("None"))) {
            return EMPTY;
        }

        return critMode.getValue();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reactionTimer.reset();
        attackTimer.reset();
        pSync.reset();
        unblocked = false;
        shouldReblockShield = false;
        hasValidTarget = false;
        lastAttackTick = -1;
        shouldAttack = false;
        targetToAttack = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        handleShieldReblock();
        shouldAttack = false;
        targetToAttack = null;
    }

    @EventSubscribe
    private void onHandleInput(EventHandleInput.Early event) {
        if (isNull()) {
            return;
        }
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) {
            return;
        }

        performAttackChecks();

        if (shouldAttack && targetToAttack != null) {
            executeAttack(targetToAttack);
            shouldAttack = false;
            targetToAttack = null;
        }
    }

    private void performAttackChecks() {
        if (isNull()) return;
        LivingEntity target = findAttackTarget();
        if (target == null) return;

        if (mc.player.isUsingItem()) {
            handleShieldUnblock();
            return;
        }

        shouldAttack = true;
        targetToAttack = target;
    }

    private LivingEntity findAttackTarget() {
        boolean attackPressed = InputHandler.isBindDown(mc.options.keyAttack);
        LivingEntity target = getCrosshairTarget();
        if (target == null) {
            resetValidTarget();
            return null;
        }

        if (!TargetUtils.isTargetAllowed(target)) {
            resetValidTarget();
            return null;
        }

        LivingEntity resolvedTarget = TargetUtils.resolveTarget(target);
        if (resolvedTarget == null || resolvedTarget != target) {
            resetValidTarget();
            return null;
        }

        if (!hasValidTarget) {
            hasValidTarget = true;
            reactionTimer.reset();
        }

        if (hurtTimeCheck.getValue() && HurtTickHandler.isCurrentlyHurt(target)) {
            return null;
        }
        if (target.isDeadOrDying() || !target.isAlive()) {
            return null;
        }
        if (target.isBlocking() && !attackShieldedTargets.getValue()) {
            return null;
        }
        if (requireClick.getValue() && !attackPressed) {
            return null;
        }
        if (!isAllowedWeapon()) {
            return null;
        }

        double cooldownRequired = attackCooldown.getRandom() / 100.0;
        if (CombatUtils.canCrit() && !critMode.is(Concat.of("None"))) {
            cooldownRequired = critCooldown.getRandom() / 100.0;
        }
        if (mc.player.getAttackStrengthScale((float) cooldownRequired) < cooldownRequired) {
            return null;
        }

        if (!reactionTimer.hasReached(reactionDelay.getValue())) {
            return null;
        }
        if (!attackTimer.hasReached(attackDelay.getValue())) {
            return null;
        }
        if (lastAttackTick == mc.player.tickCount) {
            return null;
        }
        if (!canPerformCrit(target)) {
            return null;
        }

        return target;
    }

    private LivingEntity getCrosshairTarget() {
        LivingEntity piercingTarget = getPiercingTarget();
        if (piercingTarget != null) {
            return piercingTarget;
        }

        LivingEntity hitResultTarget = getHitResultTarget();
        if (hitResultTarget != null) {
            return hitResultTarget;
        }

        LivingEntity target = CombatUtils.getCurrentItemCrosshairTarget(mc.player);
        if (target != null) {
            return target;
        }

        if (Echo.featureManager == null) {
            return null;
        }

        SpearReachModule spearReachModule = Echo.featureManager.getFeatureByClass(SpearReachModule.class);
        if (spearReachModule == null) {
            return null;
        }

        return spearReachModule.getSwapReadyCrosshairTarget();
    }

    private LivingEntity getPiercingTarget() {
        if (Echo.featureManager == null) {
            return null;
        }

        Piercing piercing = Echo.featureManager.getFeatureByClass(Piercing.class);
        if (piercing == null || !piercing.isEnabled()) {
            return null;
        }

        return piercing.findPiercingTarget();
    }

    private LivingEntity getHitResultTarget() {
        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof EntityHitResult entityHitResult)) {
            return null;
        }

        if (!(entityHitResult.getEntity() instanceof LivingEntity livingTarget)) {
            return null;
        }

        if (!livingTarget.isAlive() || livingTarget.isRemoved()) {
            return null;
        }

        return livingTarget;
    }

    private void executeAttack(LivingEntity target) {
        if (target == null || mc.player == null) return;

        mc.hitResult = new EntityHitResult(target);
        mc.crosshairPickEntity = target;
        
        InputHandler.simulateClick(mc.options.keyAttack, clickSimulation.getValue());
        attackTimer.reset();
        lastAttackTick = mc.player.tickCount;

        handleShieldReblock();
    }
    
    private void resetValidTarget() {
        if (hasValidTarget) {
            reactionTimer.reset();
            hasValidTarget = false;
        }
    }
    
    private void handleShieldUnblock() {
        if (!unshield.getValue() || unblocked) return;
        if (!PlayerUtils.isPlayerShieldingInOffHand()) return;
        if (!InputHandler.isBindDown(mc.options.keyUse)) return;

        mc.options.keyUse.setDown(false);
        unblocked = true;
        shouldReblockShield = true;
    }
    
    private void handleShieldReblock() {
        if (!unblocked) {
            shouldReblockShield = false;
            return;
        }

        if (mc.player != null && shouldReblockShield && mc.player.getOffhandItem().getItem() == Items.SHIELD) {
            mc.options.keyUse.setDown(true);
        }

        unblocked = false;
        shouldReblockShield = false;
    }
    
    private boolean checkGroundConditions() {
        return mc.player.onGround()
                || mc.player.isFallFlying()
                || mc.player.isInWater()
                || mc.player.onClimbable()
                || (requireSpaceToCrit.getValue() && !mc.options.keyJump.isDown() && !mc.player.onGround())
                || PlayerIntersectionUtil.isPlayerInWeb()
                || PlayerIntersectionUtil.isPlayerInSweetBerryBush();
    }
    
    private boolean canPerformCrit(LivingEntity target) {
        double distance = mc.player.distanceTo(target);
        float minRange = allowedCritRange.getMinValue();
        float maxRange = allowedCritRange.getMaxValue();
        if (distance < minRange || distance > maxRange) {
            return true;
        }
        
        boolean canAttack = !requireSpaceToCrit.getValue() || !mc.options.keyJump.isDown() || !mc.player.onGround();
        boolean canCrit = CombatUtils.canCrit();
        
        if (critMode.is(Concat.of("pCrit"))) {
            if (canCrit && pSync.hasReached(0) && !mc.player.isSprinting()) {
                pSync.reset();
            } else {
                canAttack = checkGroundConditions();
            }
        } else if (critMode.is(Concat.of("normal"))) {
            if (canCrit && pSync.hasReached(0)) {
                pSync.reset();
            } else {
                canAttack = checkGroundConditions();
            }
        }
        return canAttack;
    }

    private boolean isAllowedWeapon() {
        if (itemWhitelist.getSelectedCount() == 0) return true;

        ItemStack mainHand = InventoryUtils.getMainHandItem();
        if (mainHand.isEmpty()) {
            return itemWhitelist.isSelected(Items.AIR);
        }

        return itemWhitelist.isSelected(mainHand.getItem());
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onPacketRecieve(EventPacketReceive e){
        if (isNull()) return;
        Packet<?> packet = e.getPacket();
        if (packet instanceof ClientboundPlayerPositionPacket
        || packet instanceof ClientboundRespawnPacket) {
            targetToAttack = null;
            shouldAttack = false;
        }
    }

}
