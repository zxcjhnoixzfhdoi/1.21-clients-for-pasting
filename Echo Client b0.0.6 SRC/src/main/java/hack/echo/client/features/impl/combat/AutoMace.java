package hack.echo.client.features.impl.combat;

import hack.echo.client.Echo;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventMove;
import hack.echo.client.event.impl.MouseUpdateEvent;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.impl.HurtTickHandler;

import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.api.MinecraftCompat;
import hack.echo.client.utils.combat.CombatUtils;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.player.PlayerUtils;
import hack.echo.client.utils.rotation.RotationUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AutoMace extends Feature {

    private static final CharSequence RANGE_SEPARATOR = Concat.of("-");

    public AutoMace() {
        super(new FeatureInfo(
            Concat.of("Auto Mace"),
            Concat.of("Automatically attacks while holding a mace."),
            Category.COMBAT
        ));
        workWithMaceSwap.describedBy(Concat.of("With the mace swap module enabled it will allow you to hold allowed items to automatically swap to a mace."));
    }

    private final FloatSetting fallDistance = new FloatSetting(Concat.of("Fall Distance"), 1.5f, 1.5f, 9.0f, 0.1f);
    private final RangeSetting cpsRange = new RangeSetting(Concat.of("CPS"), 6f, 12f, 1f, 20f, 0.5f);
    private final BoolSetting inputSimulation = new BoolSetting(Concat.of("Input Simulation"), false);
    private final BoolSetting workWithMaceSwap = new BoolSetting(Concat.of("Work With Mace Swap"), false);

    private final BoolSetting autoAim = new BoolSetting(Concat.of("Auto Aim"), false);

    private final ModeSetting aimMath = new ModeSetting(Concat.of("Aim Math"), Concat.of("Regular"), o -> autoAim.getValue(), Concat.of("Regular"), Concat.of("Blatant"), Concat.of("WindMouse"));
    private final ModeSetting aimVector = new ModeSetting(Concat.of("Aim Vector"), Concat.of("Straight"), o -> autoAim.getValue(), Concat.of("Straight"), Concat.of("Closest"));
    private final ModeSetting targetPriority = new ModeSetting(Concat.of("Target Priority"), Concat.of("Nearest"), o -> autoAim.getValue(), Concat.of("Nearest"), Concat.of("FOV"), Concat.of("HurtTick"));
    private final BoolSetting random = new BoolSetting(Concat.of("Random"), false, o -> autoAim.getValue());
    private final BoolSetting silent = new BoolSetting(Concat.of("Silent Rotations"), false, o -> autoAim.getValue());
    private final BoolSetting throughWalls = new BoolSetting(Concat.of("Through Walls"), false, o -> autoAim.getValue());
    private final RangeSetting horizontalSpeed = new RangeSetting(Concat.of("Horizontal"), 30f, 50f, 0f, 300f, 1f, o -> autoAim.getValue());
    private final RangeSetting verticalSpeed = new RangeSetting(Concat.of("Vertical"), 30f, 50f, 0f, 300f, 1f, o -> autoAim.getValue());
    {
        silent.onChanged(() -> {
            float newUpperBound = silent.getValue() ? 600f : 300f;
            horizontalSpeed.setUpperBound(newUpperBound);
            verticalSpeed.setUpperBound(newUpperBound);
            if (horizontalSpeed.getMaxValue() > newUpperBound) horizontalSpeed.setMaxValue(newUpperBound);
            if (horizontalSpeed.getMinValue() > newUpperBound) horizontalSpeed.setMinValue(newUpperBound);
            if (verticalSpeed.getMaxValue() > newUpperBound) verticalSpeed.setMaxValue(newUpperBound);
            if (verticalSpeed.getMinValue() > newUpperBound) verticalSpeed.setMinValue(newUpperBound);
        });
    }
    private final FloatSetting range = new FloatSetting(Concat.of("Range"), 6.0f, 1.0f, 50.0f, 0.1f, o -> autoAim.getValue());
    private final FloatSetting yawFov = new FloatSetting(Concat.of("Yaw FOV"), 90.0f, 0.0f, 360.0f, 1f, o -> autoAim.getValue());
    private final FloatSetting pitchFov = new FloatSetting(Concat.of("Pitch FOV"), 90.0f, 0.0f, 180.0f, 1f, o -> autoAim.getValue());
    private final FloatSetting multipoint = new FloatSetting(Concat.of("Multipoint"), 50.0f, 0.0f, 100.0f, 1f, o -> autoAim.getValue());

    private final TimerUtils attackTimer = new TimerUtils();
    private long nextAttackDelayMs = 0L;

    @Override
    public CharSequence concat() {
        return Concat.of(
            Concat.ofFixed(cpsRange.getMinValue(), 1),
            RANGE_SEPARATOR,
            Concat.ofFixed(cpsRange.getMaxValue(), 1)
        );
    }

    @Override
    public void onDisable() {
        super.onDisable();
        RotationUtils.resetState();
    }

    @EventSubscribe
    private void onMouseUpdate(MouseUpdateEvent event) {
        if (!autoAim.getValue()) {
            if (RotationUtils.isControlledBy(this)) disengageRotations();
            return;
        }

        if (isNull() || MinecraftCompat.getScreen() != null) {
            RotationUtils.rotateBack(true, horizontalSpeed.getRandom(), verticalSpeed.getRandom(), getAimType());
            return;
        }

        if (!isFallValid()) {
            disengageRotations();
            return;
        }

        if (!isHoldingMaceOrSwap()) {
            if (silent.getValue() &&
                RotationUtils.isTracking() &&
                RotationUtils.isControlledBy(this) &&
                (MaceSwap.isSwapActive() || SpearReachModule.isSwapActive())) {
                return;
            }
            disengageRotations();
            return;
        }

        double rangeValue = range.getValue();
        double rangeSquared = rangeValue * rangeValue;
        List<LivingEntity> validTargets = mc.level.getEntitiesOfClass(
            LivingEntity.class,
            mc.player.getBoundingBox().inflate(rangeValue),
            e -> e != mc.player && !e.isDeadOrDying() && e.isAlive()
        ).stream()
            .filter(TargetUtils::isTargetAllowed)
            .filter(e -> mc.player.distanceToSqr(e) <= rangeSquared)
            .filter(this::isTargetInFov)
            .toList();

        LivingEntity candidateTarget = selectPriorityTarget(validTargets);
        LivingEntity target = TargetUtils.resolveTarget(candidateTarget);

        if (target == null || mc.player.distanceToSqr(target) > rangeSquared || !isTargetInFov(target)) {
            disengageRotations();
            return;
        }

        boolean rotationSuccess = RotationUtils.aim(this)
            .priority(EventSubscribe.Priority.HIGH)
            .speed(horizontalSpeed.getRandom(), verticalSpeed.getRandom())
            .silent(silent.getValue())
            .aimType(getAimType())
            .points(getPointsMode())
            .multipoint(multipoint.getValue())
            .random(random.getValue())
            .throughWalls(throughWalls.getValue())
            .to(target);

        if (!rotationSuccess) {
            disengageRotations();
        }
    }

    @EventSubscribe
    private void onTickInputEvent(EventHandleInput.Early event) {
        if (isNull() || MinecraftCompat.getScreen() != null) return;
        if (SwapStateManager.hasActiveSwaps()) return;

        if (!isServerFallValid()) return;

        LivingEntity target = resolveAttackTarget();
        if (target == null) return;

        if (!TargetUtils.isTargetAllowed(target)) return;

        LivingEntity resolvedTarget = TargetUtils.resolveTarget(target);
        if (resolvedTarget == null || resolvedTarget != target) return;

        if (target.isInvulnerable()) return;
        if (target.isDeadOrDying() || !target.isAlive()) return;

        if (!isHoldingMaceOrSwap()) return;

        if (!attackTimer.hasReached(nextAttackDelayMs)) return;

        mc.hitResult = new EntityHitResult(target);
        mc.crosshairPickEntity = target;
        InputHandler.simulateClick(mc.options.keyAttack, inputSimulation.getValue());

        float cps = cpsRange.getRandom();
        nextAttackDelayMs = cps <= 0f ? 0L : (long) (1000f / cps);
        attackTimer.reset();
    }

    @EventSubscribe
    private void onMove(EventMove.Pre event) {
        if (!autoAim.getValue() || !silent.getValue() || isNull()) return;
        if (!RotationUtils.hasSilentRotation()) return;

        event.setYaw(RotationUtils.getSilentYaw());
        event.setPitch(RotationUtils.getSilentPitch());
    }

    private boolean isFallValid() {
        if (mc.player.fallDistance <= fallDistance.getValue()) return false;
        return true;
    }

    private boolean isServerFallValid() {
        return PlayerUtils.getServerSyncedFallDistance(mc.player) > fallDistance.getValue();
    }

    private LivingEntity resolveAttackTarget() {
        LivingEntity piercingTarget = getPiercingTarget();
        if (piercingTarget != null) return piercingTarget;
        return getHitResultTarget();
    }

    private LivingEntity getPiercingTarget() {
        if (Echo.featureManager == null) return null;
        Piercing piercing = Echo.featureManager.getFeatureByClass(Piercing.class);
        if (piercing == null || !piercing.isEnabled()) return null;
        return piercing.findPiercingTarget();
    }

    private LivingEntity getHitResultTarget() {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY) return null;
        if (!(((EntityHitResult) mc.hitResult).getEntity() instanceof LivingEntity target)) return null;
        if (!target.isAlive() || target.isRemoved()) return null;
        return target;
    }

    private boolean isHoldingMaceOrSwap() {
        ItemStack mainHand = InventoryUtils.getMainHandItem();
        if (mainHand.is(Items.MACE)) return true;

        if (!workWithMaceSwap.getValue()) return false;

        MaceSwap maceSwap = Echo.featureManager.getFeatureByClass(MaceSwap.class);
        if (maceSwap == null || !maceSwap.isEnabled()) return false;
        if (MaceSwap.itemWhitelist.getSelectedCount() == 0) {
            return InventoryUtils.findMaceWithEnchantmentInHotbar(null, true) != -1;
        }

        boolean whitelisted = mainHand.isEmpty()
            ? MaceSwap.itemWhitelist.isSelected(Items.AIR)
            : MaceSwap.itemWhitelist.isSelected(mainHand.getItem());
        if (!whitelisted) return false;

        return InventoryUtils.findMaceWithEnchantmentInHotbar(null, true) != -1;
    }

    private void disengageRotations() {
        if (silent.getValue() && RotationUtils.isTracking() && RotationUtils.isControlledBy(this)) {
            RotationUtils.rotateBack(true, horizontalSpeed.getRandom(), verticalSpeed.getRandom(), getAimType());
        } else if (RotationUtils.isControlledBy(this)) {
            RotationUtils.stopTracking();
        }
    }

    private RotationUtils.AimType getAimType() {
        if (aimMath.is(Concat.of("WindMouse"))) return RotationUtils.AimType.WINDMOUSE;
        return aimMath.is(Concat.of("Blatant")) ? RotationUtils.AimType.BLATANT : RotationUtils.AimType.REGULAR;
    }

    private RotationUtils.EntityPoints getPointsMode() {
        if (aimVector.is(Concat.of("Closest"))) return RotationUtils.EntityPoints.CLOSEST;
        return RotationUtils.EntityPoints.STRAIGHT;
    }

    private boolean isTargetInFov(LivingEntity entity) {
        return CombatUtils.isInFOV(mc.player, entity, yawFov.getValue(), pitchFov.getValue());
    }

    private LivingEntity selectPriorityTarget(List<LivingEntity> targets) {
        if (targets == null || targets.isEmpty()) return null;

        if (targetPriority.is(Concat.of("HurtTick"))) {
            return targets.stream()
                .min(java.util.Comparator
                    .comparingLong(HurtTickHandler::getAttackedEntityTick)
                    .thenComparingDouble(e -> mc.player.distanceToSqr(e))
                    .thenComparingDouble(this::getFovDelta)
                )
                .orElse(null);
        }

        if (targetPriority.is(Concat.of("FOV"))) {
            return targets.stream()
                .min(java.util.Comparator
                    .comparingDouble(this::getFovDelta)
                    .thenComparingDouble(e -> mc.player.distanceToSqr(e))
                )
                .orElse(null);
        }

        return CombatUtils.selectNearestTarget(mc.player, targets);
    }

    private double getFovDelta(LivingEntity entity) {
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 diff = entity.getEyePosition(pt).subtract(mc.player.getEyePosition(pt));
        double horizontalDistance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, horizontalDistance)));
        float yawDiff = Math.abs(Mth.wrapDegrees(targetYaw - mc.player.getYRot()));
        float pitchDiff = Math.abs(targetPitch - mc.player.getXRot());
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }
}
