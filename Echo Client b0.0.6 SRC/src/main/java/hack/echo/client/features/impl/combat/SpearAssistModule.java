package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.api.MinecraftCompat;
import hack.echo.client.event.impl.EventMove;
import hack.echo.client.event.impl.MouseUpdateEvent;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.*;
import hack.echo.client.handlers.impl.HurtTickHandler;
import hack.echo.client.utils.combat.CombatUtils;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.rotation.RotationUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;


public class SpearAssistModule extends Feature {

	public SpearAssistModule() {
		super(new FeatureInfo(
			Concat.of("Spear Assist"),
			Concat.of("Automatically aims at targets while holding a spear"),
			Category.COMBAT
		));
	}

	private final ModeSetting aimMath = new ModeSetting(Concat.of("Aim Math"), Concat.of("Regular"), Concat.of("Regular"), Concat.of("Blatant"), Concat.of("WindMouse"));
	private final ModeSetting aimVector = new ModeSetting(Concat.of("Aim Vector"), Concat.of("Straight"), Concat.of("Straight"), Concat.of("Closest"));
	private final ModeSetting targetPriority = new ModeSetting(Concat.of("Target Priority"), Concat.of("Nearest"), Concat.of("Nearest"), Concat.of("FOV"), Concat.of("HurtTick"));
	private final BoolSetting random = new BoolSetting(Concat.of("Random"), false);
	private final BoolSetting silent = new BoolSetting(Concat.of("Silent Rotations"), false);
	private final BoolSetting throughWalls = new BoolSetting(Concat.of("Through Walls"), false);
	private final RangeSetting horizontalSpeed = new RangeSetting(Concat.of("Horizontal"), 30f, 50f, 0f, 300f, 1f);
	private final RangeSetting verticalSpeed = new RangeSetting(Concat.of("Vertical"), 30f, 50f, 0f, 300f, 1f);
	{
		silent.onChanged(() -> {
			float newUpperBound = silent.getValue() ? 600f : 300f;
			horizontalSpeed.setUpperBound(newUpperBound);
			verticalSpeed.setUpperBound(newUpperBound);
			if (horizontalSpeed.getMaxValue() > newUpperBound) {
				horizontalSpeed.setMaxValue(newUpperBound);
			}
			if (horizontalSpeed.getMinValue() > newUpperBound) {
				horizontalSpeed.setMinValue(newUpperBound);
			}
			if (verticalSpeed.getMaxValue() > newUpperBound) {
				verticalSpeed.setMaxValue(newUpperBound);
			}
			if (verticalSpeed.getMinValue() > newUpperBound) {
				verticalSpeed.setMinValue(newUpperBound);
			}
		});
	}
	private final FloatSetting range = new FloatSetting(Concat.of("Range"), 28.0f, 1.0f, 128.0f, 0.1f);
	private final FloatSetting fov = new FloatSetting(Concat.of("FOV"), 90.0f, 0.0f, 360.0f, 1f);
	private final FloatSetting multipoint = new FloatSetting(Concat.of("Multipoint"), 50.0f, 0.0f, 100.0f, 1f);

	@Override
	public CharSequence concat() {
		return aimMath.getValue();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		RotationUtils.resetState();
	}

	@EventSubscribe
	private void onRender(MouseUpdateEvent event) {
		if (isNull() || MinecraftCompat.getScreen() != null) {
			RotationUtils.rotateBack(true, horizontalSpeed.getRandom(), verticalSpeed.getRandom(), getAimType());
			return;
		}

		if (!isUsingSpear()) {
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

		if (target == null || mc.player.distanceToSqr(target) > rangeSquared) {
			disengageRotations();
			return;
		}

		if (!isTargetInFov(target)) {
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
		return CombatUtils.isInFOV(mc.player, entity, fov.getValue());
	}

	private LivingEntity selectPriorityTarget(List<LivingEntity> targets) {
		if (targets == null || targets.isEmpty()) {
			return null;
		}

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

	private boolean isUsingSpear() {
		if (isNull()) return false;
		if (!mc.player.isUsingItem()) return false;
		ItemStack using = mc.player.getUseItem();
		if (using.isEmpty()) return false;
		return InventoryUtils.isItemSpear(using.getItem());
	}

	@EventSubscribe
	private void onMove(EventMove.Pre event) {
		if (!silent.getValue() || isNull()) return;
		if (!RotationUtils.hasSilentRotation()) return;

		event.setYaw(RotationUtils.getSilentYaw());
		event.setPitch(RotationUtils.getSilentPitch());
	}
}
