package hack.echo.client.utils.simulation;

import hack.echo.client.utils.Imports;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CrossbowSimulation implements Imports {

    private static final double ARROW_POWER = 3.15;
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

    public CrossbowSimulation(Player player, float spreadDegrees, float partialTick) {
        Vec3 launchDirection = Vec3.directionFromRotation(player.getViewXRot(partialTick), player.getViewYRot(partialTick)).normalize();
        Vec3 up = player.getUpVector(partialTick).normalize();
        Vec3 rotatedDirection = rotateAroundAxis(launchDirection, up, spreadDegrees);

        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight();
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        this.position = new Vec3(x, y, z);
        this.velocity = rotatedDirection.scale(ARROW_POWER);

        Vec3 playerVel = player.getDeltaMovement();
        this.velocity = velocity.add(playerVel.x, player.onGround() ? 0.0 : playerVel.y, playerVel.z);

        this.sourceEntity = player;
        this.ownerEntity = player;
        this.leftOwner = false;
        this.hit = false;
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
        velocity = velocity.subtract(0.0, GRAVITY, 0.0);
    }

    public Vec3 getPosition() {
        return position;
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

    private void updateLeftOwner(Vec3 nextPos) {
        if (leftOwner || ownerEntity == null) return;

        AABB ownerCheckBox = new AABB(position, nextPos).inflate(ENTITY_CHECK_MARGIN);
        leftOwner = ownerEntity
            .getRootVehicle()
            .getSelfAndPassengers()
            .noneMatch(passenger -> ownerCheckBox.intersects(passenger.getBoundingBox()));
    }

    private static Vec3 rotateAroundAxis(Vec3 vector, Vec3 axis, float angleDegrees) {
        if (Mth.equal(angleDegrees, 0.0F)) return vector;

        Vec3 k = axis.normalize();
        double angleRadians = angleDegrees * Mth.DEG_TO_RAD;
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        return vector.scale(cos)
            .add(k.cross(vector).scale(sin))
            .add(k.scale(k.dot(vector) * (1.0 - cos)));
    }
}
