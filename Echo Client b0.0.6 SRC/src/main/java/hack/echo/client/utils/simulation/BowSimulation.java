package hack.echo.client.utils.simulation;

import hack.echo.client.utils.Imports;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BowSimulation implements Imports {

    private static final double GRAVITY = 0.05;
    private static final float INERTIA = 0.99F;
    private static final float WATER_INERTIA = 0.6F;
    private static final double ENTITY_CHECK_MARGIN = 1.0D;

    private Vec3 position;
    private Vec3 velocity;
    private final Entity sourceEntity;
    private final Entity ownerEntity;
    private boolean leftOwner;
    private boolean hit;

    public BowSimulation(Player player, int chargeTicks, float partialTick) {
        float power = BowItem.getPowerForTime(chargeTicks);

        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight();
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        this.position = new Vec3(x, y, z);
        this.velocity = getArrowVelocity(player.getViewXRot(partialTick), player.getViewYRot(partialTick), power * 3.0f);

        // Add player momentum
        Vec3 playerVel = player.getDeltaMovement();
        this.velocity = velocity.add(playerVel.x, player.onGround() ? 0 : playerVel.y, playerVel.z);
        this.sourceEntity = player;
        this.ownerEntity = player;
        this.leftOwner = false;
        this.hit = false;
    }

    public BowSimulation(Vec3 position, float pitch, float yaw, float power) {
        this.position = position;
        this.velocity = getArrowVelocity(pitch, yaw, power * 3.0f);
        this.sourceEntity = mc.player;
        this.ownerEntity = resolveOwner(mc.player);
        this.leftOwner = this.ownerEntity == null;
        this.hit = false;
    }

    private Vec3 getArrowVelocity(float pitch, float yaw, float power) {
        float x = -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
        float y = -Mth.sin(pitch * Mth.DEG_TO_RAD);
        float z = Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
        return new Vec3(x, y, z).normalize().scale(power);
    }

    public void simulateTick() {
        if (hit || mc.level == null) return;

        boolean inWater = !mc.level.getFluidState(BlockPos.containing(position)).isEmpty();
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
        
        velocity = velocity.scale(inWater ? WATER_INERTIA : INERTIA);
        velocity = velocity.subtract(0, GRAVITY, 0);
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
