package hack.echo.client.features.impl.combat;

import hack.echo.client.api.AttackRangeCompat;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.RegistryPickerSetting;
import hack.echo.client.mixin.accessors.KeyMappingAccessor;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class Piercing extends Feature {

	public final BoolSetting blocks = new BoolSetting(Concat.of("Blocks"), true);
    public final BoolSetting interact = new BoolSetting(Concat.of("Interact"), false);
	public final ModeSetting sortBy = new ModeSetting(
		Concat.of("Sort Targets By"), Concat.of("Hurt Time"),
		Concat.of("Hurt Time"), Concat.of("Health")
	);

    public final RegistryPickerSetting<Block> holdingBlock = new RegistryPickerSetting<>(Concat.of("Holding Block"), () -> BuiltInRegistries.BLOCK) {
        @Override
        public Function<Block, String> getNameProvider() {
            return s -> new ItemStack(s.asItem()).getHoverName().getString();
        }

        @Override
        public Predicate<Block> getEntryFilter() {
            return block -> block.defaultBlockState().getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
        }

        @Override
        public boolean isVisible() {
            return interact.getValue();
        }
		{
			setSelected(Blocks.COBWEB, true);
		}
    };

	public Piercing() {
		super(new FeatureInfo(
			Concat.of("Piercing"),
			Concat.of("Hit through entities and blocks"),
			Category.COMBAT
		));
	}

    private InteractionHand getHoldingHand() {
        var mainItem = mc.player.getMainHandItem().getItem();
        var offhandItem = mc.player.getOffhandItem().getItem();

        if (mainItem instanceof BlockItem blockItem) {
            var block = blockItem.getBlock();
            if (holdingBlock.isSelected(block)) {
                return InteractionHand.MAIN_HAND;
            }
        }
        if (offhandItem instanceof BlockItem blockItem) {
            var block = blockItem.getBlock();
            if (holdingBlock.isSelected(block)) {
                return InteractionHand.OFF_HAND;
            }
        }
        return null;
    }

	@EventSubscribe
	private void onHandleInputEarly(EventHandleInput.Early event) {
		if (mc.hitResult == null) return;

		Trace trace = getTrace();
		if (trace == null) return;

		boolean attackPending = mc.options.keyAttack.isDown()
				|| ((KeyMappingAccessor) mc.options.keyAttack).getClickCount() > 0;
		if (attackPending) {
			TargetHit targetHit = findTargetHit(trace);
			if (targetHit == null) return;

			setCrosshairTarget(targetHit);
			return;
		}

		if (interact.getValue() && mc.options.keyUse.isDown() && getHoldingHand() != null) {
			var hit = raycastBlock();
			if (hit.getType() == HitResult.Type.BLOCK) {
				mc.hitResult = hit;
				return;
			}
		}

		restoreBlockHit(trace);
	}

	public LivingEntity findPiercingTarget() {
		TargetHit targetHit = findPiercingHit();
		if (targetHit == null) return null;

		return targetHit.target();
	}

	public EntityHitResult findPiercingHitResult() {
		TargetHit targetHit = findPiercingHit();
		if (targetHit == null) return null;

		return targetHit.toHitResult();
	}

	private TargetHit findPiercingHit() {
		Trace trace = getTrace();
		if (trace == null) return null;

		return findTargetHit(trace);
	}

	private TargetHit findTargetHit(Trace trace) {
		double entityRangeSq = getEntityRangeSq(trace);
		List<TargetHit> targets = collectTargets(trace, entityRangeSq);
		if (targets.isEmpty()) return null;

		return selectBest(targets);
	}

	private Trace getTrace() {
		if (!isEnabled()) return null;
		if (isNull()) return null;

		LocalPlayer player = mc.player;
		float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 start = player.getEyePosition(partialTick);
		Vec3 end = start.add(player.getViewVector(partialTick).scale(getMaxRange(player)));

		return new Trace(player, start, end);
	}

	private double getMaxRange(LocalPlayer player) {
		double attackRange = AttackRangeCompat.getMaxAttackRange(player);
		return Math.max(attackRange, player.blockInteractionRange());
	}

	private double getEntityRangeSq(Trace trace) {
		LocalPlayer player = trace.player();
		double maxAttackRange = AttackRangeCompat.getMaxAttackRange(player);
		double entityRangeSq = maxAttackRange * maxAttackRange;
		if (blocks.getValue()) return entityRangeSq;

		BlockHitResult blockHit = clipBlocks(trace);
		if (blockHit.getType() == HitResult.Type.MISS) {
			return entityRangeSq;
		}

		double blockDistSq = trace.start().distanceToSqr(blockHit.getLocation());
		if (blockDistSq < entityRangeSq) {
			return blockDistSq;
		}

		return entityRangeSq;
	}

    private BlockHitResult raycastBlock() {
        Vec3 start = mc.player.getEyePosition();
        double yawRad = Math.toRadians(-mc.player.getYRot());
        double pitchRad = Math.toRadians(-mc.player.getXRot());
        Vec3 direction = new Vec3(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        );
        double range = mc.player.blockInteractionRange();
        Vec3 end = start.add(direction.scale(range));

        return clipBlocks(new Trace(mc.player, start, end));
    }

	private BlockHitResult clipBlocks(Trace trace) {
		return mc.level.clip(new ClipContext(
			trace.start(),
			trace.end(),
			ClipContext.Block.OUTLINE,
			ClipContext.Fluid.NONE,
			trace.player()
		));
	}

	private void setCrosshairTarget(TargetHit targetHit) {
		mc.hitResult = targetHit.toHitResult();
		mc.crosshairPickEntity = targetHit.target();
	}

	private void restoreBlockHit(Trace trace) {
		if (!(mc.hitResult instanceof EntityHitResult entityHit)) return;
		if (entityHit.getEntity() instanceof LivingEntity living && TargetUtils.isTargetAllowed(living)) return;

		BlockHitResult blockHit = clipBlocks(trace);
		if (blockHit.getType() != HitResult.Type.MISS
				&& blockHit.getLocation().distanceToSqr(trace.start()) <= trace.player().blockInteractionRange() * trace.player().blockInteractionRange()) {
			mc.hitResult = blockHit;
		}

		mc.crosshairPickEntity = null;
	}

	private List<TargetHit> collectTargets(Trace trace, double maxRangeSq) {
		List<TargetHit> targets = new ArrayList<>();
		AABB searchBox = trace.player().getBoundingBox().expandTowards(trace.end().subtract(trace.start())).inflate(1.0, 1.0, 1.0);

		for (Entity entity : mc.level.getEntities(trace.player(), searchBox, EntitySelector.CAN_BE_PICKED)) {
			if (!(entity instanceof LivingEntity living)) continue;
			if (!TargetUtils.isTargetAllowed(living)) continue;

			AABB hitbox = entity.getBoundingBox().inflate(entity.getPickRadius());
			Optional<Vec3> clip = hitbox.clip(trace.start(), trace.end());
			if (clip.isEmpty()) continue;

			Vec3 hitLocation = clip.get();
			double distSq = trace.start().distanceToSqr(hitLocation);
			if (distSq > maxRangeSq) continue;
			if (!AttackRangeCompat.isLocationInRange(trace.player(), hitLocation)) continue;

			targets.add(new TargetHit(living, hitLocation));
		}

		return targets;
	}

	private TargetHit selectBest(List<TargetHit> targets) {
		if (targets.size() == 1) return targets.get(0);

		if (sortBy.is(Concat.of("Hurt Time"))) {
			targets.sort(Comparator.comparingInt(t -> t.target().hurtTime));
		} else {
			targets.sort(Comparator.comparingDouble(t -> t.target().getHealth()));
		}

		return targets.get(0);
	}

	private record Trace(LocalPlayer player, Vec3 start, Vec3 end) {
	}

	private record TargetHit(LivingEntity target, Vec3 location) {

		private EntityHitResult toHitResult() {
			return new EntityHitResult(target, location);
		}
	}
}
