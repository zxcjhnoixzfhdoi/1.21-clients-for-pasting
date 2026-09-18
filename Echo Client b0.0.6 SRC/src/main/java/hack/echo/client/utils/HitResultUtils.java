package hack.echo.client.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static hack.echo.client.utils.Imports.mc;

public final class HitResultUtils {

    private HitResultUtils() {}

    /**
     * Creates a MISS hit result from the player's current eye position and direction
     */
    public static HitResult createMissFromPlayer() {
        if (mc.player == null) return null;
        return createMiss(mc.player.getEyePosition(), mc.player.getDirection());
    }

    /**
     * Creates a MISS hit result at the specified position and direction
     */
    public static HitResult createMiss(Vec3 position, Direction direction) {
        if (position == null || direction == null) return null;
        return BlockHitResult.miss(position, direction, BlockPos.containing(position));
    }

    /**
     * Creates a MISS hit result at the specified position with UP direction
     */
    public static HitResult createMissAt(Vec3 position) {
        return createMiss(position, Direction.UP);
    }

    /**
     * Creates a MISS hit result at the specified block position
     */
    public static HitResult createMissAt(BlockPos pos) {
        if (pos == null) return null;
        Vec3 center = Vec3.atCenterOf(pos);
        return createMiss(center, Direction.UP);
    }

    /**
     * Creates a BlockHitResult for the specified block position
     */
    public static BlockHitResult createBlockHit(BlockPos pos, Direction direction) {
        if (pos == null || direction == null) return null;
        Vec3 hitVec = Vec3.atCenterOf(pos);
        return new BlockHitResult(hitVec, direction, pos, false);
    }

    /**
     * Creates a BlockHitResult for the specified block position with hit vector
     */
    public static BlockHitResult createBlockHit(BlockPos pos, Direction direction, Vec3 hitVec, boolean inside) {
        if (pos == null || direction == null || hitVec == null) return null;
        return new BlockHitResult(hitVec, direction, pos, inside);
    }

    /**
     * Creates an EntityHitResult for the specified entity
     */
    public static EntityHitResult createEntityHit(Entity entity) {
        if (entity == null) return null;
        Vec3 location = entity.getBoundingBox().getCenter();
        return new EntityHitResult(entity, location);
    }

    /**
     * Creates an EntityHitResult for the specified entity with custom location
     */
    public static EntityHitResult createEntityHit(Entity entity, Vec3 location) {
        if (entity == null || location == null) return null;
        return new EntityHitResult(entity, location);
    }

    /**
     * Checks if the hit result is targeting a block
     */
    public static boolean isBlockHit(HitResult hitResult) {
        return hitResult != null && hitResult.getType() == HitResult.Type.BLOCK;
    }

    /**
     * Checks if the hit result is targeting an entity
     */
    public static boolean isEntityHit(HitResult hitResult) {
        return hitResult != null && hitResult.getType() == HitResult.Type.ENTITY;
    }

    /**
     * Checks if the hit result is a miss
     */
    public static boolean isMiss(HitResult hitResult) {
        return hitResult == null || hitResult.getType() == HitResult.Type.MISS;
    }

    /**
     * Gets the entity from a hit result, or null if not an entity hit
     */
    public static Entity getEntity(HitResult hitResult) {
        if (!isEntityHit(hitResult)) return null;
        return ((EntityHitResult) hitResult).getEntity();
    }

    /**
     * Gets the block position from a hit result, or null if not a block hit
     */
    public static BlockPos getBlockPos(HitResult hitResult) {
        if (!isBlockHit(hitResult)) return null;
        return ((BlockHitResult) hitResult).getBlockPos();
    }

    /**
     * Gets the hit location from a hit result
     */
    public static Vec3 getLocation(HitResult hitResult) {
        if (hitResult == null) return null;
        return hitResult.getLocation();
    }

    /**
     * Gets the direction from a block hit result, or null if not a block hit
     */
    public static Direction getDirection(HitResult hitResult) {
        if (!isBlockHit(hitResult)) return null;
        return ((BlockHitResult) hitResult).getDirection();
    }

    /**
     * Checks if the hit result is inside a block (for block hits)
     */
    public static boolean isInsideBlock(HitResult hitResult) {
        if (!isBlockHit(hitResult)) return false;
        return ((BlockHitResult) hitResult).isInside();
    }

    /**
     * Spoofs the hit result to MISS if it's currently targeting an entity
     * Returns the original hit result for restoration
     */
    public static HitResult spoofEntityToMiss() {
        if (mc.hitResult == null || !isEntityHit(mc.hitResult)) {
            return mc.hitResult;
        }
        HitResult original = mc.hitResult;
        HitResult miss = createMissFromPlayer();
        if (miss != null) {
            mc.hitResult = miss;
        }
        return original;
    }

    /**
     * Restores a previously saved hit result
     */
    public static void restoreHitResult(HitResult hitResult) {
        mc.hitResult = hitResult;
    }

    /**
     * Temporarily sets hit result to MISS, executes action, then restores
     */
    public static void withMissHitResult(Runnable action) {
        HitResult original = spoofEntityToMiss();
        try {
            action.run();
        } finally {
            restoreHitResult(original);
        }
    }

    /**
     * Gets the distance from player to hit location
     */
    public static double getDistanceToHit(HitResult hitResult) {
        if (mc.player == null || hitResult == null) return Double.MAX_VALUE;
        Vec3 location = hitResult.getLocation();
        return mc.player.getEyePosition().distanceTo(location);
    }

    /**
     * Gets the distance from player to hit entity
     */
    public static double getDistanceToEntity(HitResult hitResult) {
        Entity entity = getEntity(hitResult);
        if (mc.player == null || entity == null) return Double.MAX_VALUE;
        return mc.player.distanceTo(entity);
    }

    /**
     * Gets the distance from player to hit block
     */
    public static double getDistanceToBlock(HitResult hitResult) {
        BlockPos pos = getBlockPos(hitResult);
        if (mc.player == null || pos == null) return Double.MAX_VALUE;
        return mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos));
    }
}

