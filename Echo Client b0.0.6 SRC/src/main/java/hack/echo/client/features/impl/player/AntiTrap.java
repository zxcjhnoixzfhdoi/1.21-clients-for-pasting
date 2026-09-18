package hack.echo.client.features.impl.player;

import hack.echo.client.Echo;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventBlockBreaking;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventOnAttackEntity;
import hack.echo.client.event.impl.EventPerformUseItemOn;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.RegistryPickerSetting;
import hack.echo.client.utils.inventory.BlockGroups;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

public class AntiTrap extends Feature {

    private final BoolSetting noInteract = new BoolSetting(Concat.of("No Interact"), true);

    private final BoolSetting armorStands = new BoolSetting(Concat.of("Armor Stands"), true);
    private final BoolSetting itemFrames = new BoolSetting(Concat.of("Item Frames"), true);
    private final BoolSetting glowItemFrames = new BoolSetting(Concat.of("Glow Frames"), true);
    private final BoolSetting paintings = new BoolSetting(Concat.of("Paintings"), true);
    private final BoolSetting signs = new BoolSetting(Concat.of("Signs"), true);
    private final BoolSetting hangingSigns = new BoolSetting(Concat.of("Hanging Signs"), true);

    private final RegistryPickerSetting<Block> blocks = new RegistryPickerSetting<>(Concat.of("Blocks"), () -> BuiltInRegistries.BLOCK) {
        @Override
        public List<EntryGroup<Block>> getGroups() {
            return BlockGroups.getAllGroups();
        }

        @Override
        public Function<Block, String> getNameProvider() {
            return block -> new ItemStack(block.asItem()).getHoverName().getString();
        }
    };

    public AntiTrap() {
        super(new FeatureInfo(
                Concat.of("Anti Trap"),
                Concat.of("Helps you to not get trapped."),
                Category.UTILITY
        ));
    }

    @EventSubscribe
    private void onHandleInputEarly(EventHandleInput.Early event) {
        if (!noInteract.getValue()) {
            return;
        }

        if (isNull()) {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }

        if (!matchesBlock(mc.level.getBlockState(blockHitResult.getBlockPos()))) {
            return;
        }

        HitResult result = pickPastBlockedBlocks();
        mc.hitResult = result;
        mc.crosshairPickEntity = result instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
    }

    @EventSubscribe
    private void onStartAttack(EventStartAttack event) {
        if (!shouldCancelCurrentBlockInteraction()) {
            return;
        }

        event.cancel();
    }

    @EventSubscribe
    private void onBlockBreaking(EventBlockBreaking.Pre event) {
        if (!shouldCancelCurrentBlockInteraction()) {
            return;
        }

        event.cancel();
    }

    @EventSubscribe
    private void onUseItemOn(EventPerformUseItemOn.Pre event) {
        if (!shouldCancelBlockInteraction(event.getHitResult())) {
            return;
        }

        event.cancel();
    }

    @EventSubscribe
    private void onAttackEntity(EventOnAttackEntity.Pre event) {
        if (!shouldCancelEntityInteraction(event.getTarget())) {
            return;
        }

        event.cancel();
    }

    private boolean shouldCancelCurrentBlockInteraction() {
        if (!noInteract.getValue()) {
            return false;
        }

        if (isNull()) {
            return false;
        }

        if (!(mc.hitResult instanceof BlockHitResult hitResult)) {
            return false;
        }

        return shouldCancelBlockInteraction(hitResult);
    }

    private boolean shouldCancelBlockInteraction(BlockHitResult hitResult) {
        if (!noInteract.getValue()) {
            return false;
        }

        if (isNull()) {
            return false;
        }

        if (hitResult == null) {
            return false;
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockState state = mc.level.getBlockState(hitResult.getBlockPos());
        return matchesBlock(state);
    }

    private boolean shouldCancelEntityInteraction(Entity entity) {
        if (!noInteract.getValue()) {
            return false;
        }

        if (entity == null) {
            return false;
        }

        return matchesEntity(entity.getType());
    }

    private HitResult pickPastBlockedBlocks() {
        LocalPlayer player = mc.player;
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eyePosition = player.getEyePosition(pt);
        Vec3 lookDirection = player.getViewVector(pt);

        double blockRange = player.blockInteractionRange();
        double entityRange = player.entityInteractionRange();
        double maxRange = Math.max(blockRange, entityRange);

        Vec3 maxEnd = eyePosition.add(lookDirection.scale(maxRange));
        BlockHitResult blockHitResult = clipPastBlockedBlocks(eyePosition, maxEnd, player);

        double maxDistanceSq = maxRange * maxRange;
        double entityDistance = maxRange;
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            maxDistanceSq = eyePosition.distanceToSqr(blockHitResult.getLocation());
            entityDistance = Math.sqrt(maxDistanceSq);
        }

        Vec3 entityEnd = eyePosition.add(lookDirection.scale(entityDistance));
        AABB entityBox = player.getBoundingBox().expandTowards(lookDirection.scale(entityDistance)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                player,
                eyePosition,
                entityEnd,
                entityBox,
                candidate -> EntitySelector.CAN_BE_PICKED.test(candidate) && !matchesEntity(candidate.getType()),
                maxDistanceSq
        );

        if (entityHitResult != null && entityHitResult.getLocation().distanceToSqr(eyePosition) < maxDistanceSq) {
            return limitInteractionRange(entityHitResult, eyePosition, entityRange);
        }

        if (blockHitResult.getType() != HitResult.Type.MISS) {
            return limitInteractionRange(blockHitResult, eyePosition, blockRange);
        }

        return createMissHit(maxEnd, lookDirection);
    }

    private BlockHitResult clipPastBlockedBlocks(Vec3 start, Vec3 end, LocalPlayer player) {
        Vec3 currentStart = start;

        for (int i = 0; i < 16; i++) {
            BlockHitResult hitResult = mc.level.clip(new ClipContext(
                    currentStart,
                    end,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (hitResult.getType() == HitResult.Type.MISS) {
                return createMissHit(end, end.subtract(start));
            }

            if (!matchesBlock(mc.level.getBlockState(hitResult.getBlockPos()))) {
                return hitResult;
            }

            currentStart = movePastHit(hitResult.getLocation(), start, end);
        }

        return createMissHit(end, end.subtract(start));
    }

    private Vec3 movePastHit(Vec3 hitLocation, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() == 0.0) {
            return hitLocation;
        }

        return hitLocation.add(direction.normalize().scale(0.001));
    }

    private HitResult limitInteractionRange(HitResult hitResult, Vec3 eyePosition, double range) {
        Vec3 hitLocation = hitResult.getLocation();
        if (hitLocation.closerThan(eyePosition, range)) {
            return hitResult;
        }

        return createMissHit(hitLocation, hitLocation.subtract(eyePosition));
    }

    private BlockHitResult createMissHit(Vec3 position, Vec3 directionVector) {
        Direction direction = Direction.getApproximateNearest(directionVector.x, directionVector.y, directionVector.z);
        return BlockHitResult.miss(position, direction, BlockPos.containing(position));
    }

    private boolean matchesEntity(EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }

        if (armorStands.getValue() && entityType == EntityType.ARMOR_STAND) {
            return true;
        }

        if (itemFrames.getValue() && entityType == EntityType.ITEM_FRAME) {
            return true;
        }

        if (glowItemFrames.getValue() && entityType == EntityType.GLOW_ITEM_FRAME) {
            return true;
        }

        if (paintings.getValue() && entityType == EntityType.PAINTING) {
            return true;
        }

        return false;
    }

    public static boolean shouldBlockEntityInteraction(Entity entity) {
        if (Echo.isDestroyed || Echo.featureManager == null) {
            return false;
        }

        AntiTrap antiTrap = Echo.featureManager.getFeatureByClass(AntiTrap.class);
        if (antiTrap == null || !antiTrap.isEnabled()) {
            return false;
        }

        if (!antiTrap.noInteract.getValue()) {
            return false;
        }

        if (entity == null) {
            return false;
        }

        return antiTrap.matchesEntity(entity.getType());
    }

    private boolean matchesBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        if (signs.getValue() && (state.is(BlockTags.STANDING_SIGNS) || state.is(BlockTags.WALL_SIGNS))) {
            return true;
        }

        if (hangingSigns.getValue() && (state.is(BlockTags.CEILING_HANGING_SIGNS) || state.is(BlockTags.WALL_HANGING_SIGNS))) {
            return true;
        }

        return blocks.isSelected(state.getBlock());
    }
}
