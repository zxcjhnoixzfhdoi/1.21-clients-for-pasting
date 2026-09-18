package hack.echo.client.utils.simulation;

import hack.echo.client.utils.Imports;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ArrowSimulation implements Imports {

    private static final double GRAVITY = 0.05;
    private static final float INERTIA = 0.99F;
    private static final float WATER_INERTIA = 0.6F;
    private static final double ENTITY_CHECK_MARGIN = 1.0D;

    private Vec3 position;
    private Vec3 velocity;
    private final Entity sourceEntity;
    private final Entity ownerEntity;
    private boolean leftOwner;
    private boolean inGround;

    public ArrowSimulation(Vec3 position, Vec3 velocity) {
        this(position, velocity, null);
    }

    public ArrowSimulation(Vec3 position, Vec3 velocity, Entity ignoredEntity) {
        this.position = position;
        this.velocity = velocity;
        this.sourceEntity = ignoredEntity;
        this.ownerEntity = resolveOwner(ignoredEntity);
        this.leftOwner = this.ownerEntity == null;
        this.inGround = false;
    }

    public void simulateTick() {
        if (inGround || mc.level == null) return;

        boolean inWater = !mc.level.getFluidState(BlockPos.containing(position)).isEmpty();
        Vec3 nextPos = position.add(velocity);
        updateLeftOwner(nextPos);
        
        BlockHitResult hit = mc.level.clip(new ClipContext(
            position, nextPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            sourceEntity != null ? sourceEntity : mc.player
        ));

        EntityHitResult entityHit = raycastEntity(nextPos);

        if (entityHit != null && (hit.getType() == HitResult.Type.MISS
            || position.distanceToSqr(entityHit.getLocation()) <= position.distanceToSqr(hit.getLocation()))) {
            position = entityHit.getLocation();
            inGround = true;
            return;
        }

        if (hit.getType() != HitResult.Type.MISS) {
            position = hit.getLocation();
            inGround = true;
            return;
        }

        position = nextPos;
        
        velocity = velocity.scale(inWater ? WATER_INERTIA : INERTIA);
        velocity = velocity.subtract(0, GRAVITY, 0);
    }

    public void simulateTicks(int ticks) {
        for (int i = 0; i < ticks && !inGround; i++) {
            simulateTick();
        }
    }

    public BlockHitResult simulateUntilHit(int maxTicks) {
        for (int i = 0; i < maxTicks && !inGround; i++) {
            Vec3 prevPos = position;
            simulateTick();
            if (inGround) {
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

    public boolean isInGround() {
        return inGround;
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
        if (entity instanceof Player target && ownerEntity instanceof Player owner && !owner.canHarmPlayer(target)) return false;
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
