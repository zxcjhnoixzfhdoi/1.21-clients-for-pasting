package hack.echo.client.utils.simulation;

import hack.echo.client.utils.Imports;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PearlSimulation implements Imports {

    private static final double GRAVITY = 0.03;
    private static final float INERTIA = 0.99F;
    private static final float WATER_INERTIA = 0.8F;
    private static final double ENTITY_CHECK_MARGIN = 1.0D;

    private Vec3 position;
    private Vec3 velocity;
    private final Entity sourceEntity;
    private final Entity ownerEntity;
    private boolean leftOwner;
    private boolean hit;

    public PearlSimulation(Vec3 position, Vec3 velocity) {
        this(position, velocity, mc.player);
    }

    public PearlSimulation(Vec3 position, Vec3 velocity, Entity ignoredEntity) {
        this.position = position;
        this.velocity = velocity;
        this.sourceEntity = ignoredEntity;
        this.ownerEntity = resolveOwner(ignoredEntity);
        this.leftOwner = this.ownerEntity == null;
        this.hit = false;
    }

    public void simulateTick() {
        if (hit || mc.level == null) return;

        boolean inWater = !mc.level.getFluidState(BlockPos.containing(position)).isEmpty();
        velocity = velocity.subtract(0, GRAVITY, 0);
        velocity = velocity.scale(inWater ? WATER_INERTIA : INERTIA);
        Vec3 nextPos = position.add(velocity);
        updateLeftOwner(nextPos);
        
        BlockHitResult blockHit = mc.level.clip(new ClipContext(
            position, nextPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            sourceEntity != null ? sourceEntity : mc.player
        ));

        EntityHitResult entityHit = raycastEntity(nextPos);

        if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS
            || position.distanceToSqr(entityHit.getLocation()) <= position.distanceToSqr(blockHit.getLocation()))) {
            position = entityHit.getLocation();
            hit = true;
            return;
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            position = blockHit.getLocation();
            hit = true;
            return;
        }

        position = nextPos;
    }

    public void simulateTicks(int ticks) {
        for (int i = 0; i < ticks && !hit; i++) {
            simulateTick();
        }
    }

    public BlockHitResult simulateUntilHit(int maxTicks) {
        for (int i = 0; i < maxTicks && !hit; i++) {
            Vec3 prevPos = position;
            simulateTick();
            if (hit) {
                return mc.level.clip(new ClipContext(
                    prevPos, position,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mc.player
                ));
            }
        }
        return null;
    }

    public Vec3 getPosition() {
        return position;
    }

    public Vec3 getVelocity() {
        return velocity;
    }

    public boolean hasHit() {
        return hit;
    }

    private EntityHitResult raycastEntity(Vec3 nextPos) {
        AABB searchBox = new AABB(position, nextPos).inflate(ENTITY_CHECK_MARGIN);

        return ProjectileUtil.getEntityHitResult(
            sourceEntity != null ? sourceEntity : mc.player,
            position,
            nextPos,
            searchBox,
            this::canHitEntity,
            position.distanceToSqr(nextPos)
        );
    }

    private boolean canHitEntity(Entity entity) {
        if (!entity.canBeHitByProjectile()) return false;
        return ownerEntity == null || leftOwner || !ownerEntity.isPassengerOfSameVehicle(entity);
    }

    private Entity resolveOwner(Entity sourceEntity) {
        if (sourceEntity instanceof Projectile projectile) {
            return projectile.getOwner();
        }

        return sourceEntity;
    }

    private void updateLeftOwner(Vec3 nextPos) {
        if (leftOwner || ownerEntity == null) return;

        AABB ownerCheckBox = new AABB(position, nextPos).inflate(ENTITY_CHECK_MARGIN);
        leftOwner = ownerEntity
            .getRootVehicle()
            .getSelfAndPassengers()
            .noneMatch(passenger -> ownerCheckBox.intersects(passenger.getBoundingBox()));
    }
}
