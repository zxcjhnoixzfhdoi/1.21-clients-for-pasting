package hack.echo.client.utils.simulation;

import hack.echo.client.utils.Imports;
import hack.echo.client.mixin.accessors.FireworkRocketAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CrossbowFireworkSimulation implements Imports {

    private static final float FIREWORK_POWER = 1.6F;
    private static final float INACCURACY = 1.0F;
    private static final double ENTITY_CHECK_MARGIN = 1.0D;

    private Vec3 position;
    private Vec3 velocity;
    private final Entity sourceEntity;
    private final Entity ownerEntity;
    private final boolean shotAtAngle;
    private final int lifetime;
    private int life;
    private boolean leftOwner;
    private boolean hit;

    public CrossbowFireworkSimulation(Player player, float spreadDegrees, ItemStack fireworkItem, int shotIndex, float partialTick) {
        Vec3 launchDirection = Vec3.directionFromRotation(player.getViewXRot(partialTick), player.getViewYRot(partialTick)).normalize();
        Vec3 up = player.getUpVector(partialTick).normalize();
        Vec3 rotatedDirection = rotateAroundAxis(launchDirection, up, spreadDegrees);

        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight() - 0.15F;
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        this.position = new Vec3(x, y, z);
        this.velocity = applyInaccuracy(rotatedDirection, shotSeed(player, fireworkItem, shotIndex)).scale(FIREWORK_POWER);

        Vec3 playerVel = player.getDeltaMovement();
        this.velocity = velocity.add(playerVel.x, player.onGround() ? 0.0 : playerVel.y, playerVel.z);

        this.sourceEntity = player;
        this.ownerEntity = player;
        this.shotAtAngle = true;
        this.leftOwner = false;
        this.life = 0;
        this.lifetime = resolveLifetime(fireworkItem, shotSeed(player, fireworkItem, shotIndex) ^ 0x6A09E667F3BCC909L);
        this.hit = false;
    }

    public CrossbowFireworkSimulation(FireworkRocketEntity firework) {
        this.position = firework.position();
        this.velocity = firework.getDeltaMovement();
        this.sourceEntity = firework;
        this.ownerEntity = firework instanceof Projectile projectile ? projectile.getOwner() : firework;
        this.shotAtAngle = firework.isShotAtAngle();

        int syncedLife = ((FireworkRocketAccessor) firework).echo$getLife();
        int syncedLifetime = ((FireworkRocketAccessor) firework).echo$getLifetime();
        boolean hasSyncedLifetime = syncedLifetime > 0;

        this.life = Math.max(0, hasSyncedLifetime ? syncedLife : firework.tickCount);
        this.lifetime = hasSyncedLifetime
            ? syncedLifetime
            : resolveLifetime(firework.getItem(), entitySeed(firework) ^ 0x6A09E667F3BCC909L);
        this.leftOwner = this.ownerEntity == null;
        this.hit = false;
    }

    public void simulateTick() {
        if (hit || mc.level == null) return;

        if (!shotAtAngle) {
            velocity = velocity.multiply(1.15, 1.0, 1.15).add(0.0, 0.04, 0.0);
        }

        Vec3 nextPos = position.add(velocity);
        updateLeftOwner(nextPos);

        BlockHitResult blockHit = mc.level.clip(new ClipContext(
            position,
            nextPos,
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
        life++;
        if (life > lifetime) {
            hit = true;
        }
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

    private static Vec3 applyInaccuracy(Vec3 direction, long seed) {
        if (Mth.equal(INACCURACY, 0.0F)) return direction.normalize();

        RandomSource random = RandomSource.create(seed);
        double spread = 0.0172275 * INACCURACY;
        Vec3 adjusted = direction.normalize().add(
            random.triangle(0.0, spread),
            random.triangle(0.0, spread),
            random.triangle(0.0, spread)
        );
        return adjusted.normalize();
    }

    private static int resolveLifetime(ItemStack fireworkItem, long seed) {
        int flight = 1;
        Fireworks fireworks = fireworkItem.get(DataComponents.FIREWORKS);
        if (fireworks != null) {
            flight += fireworks.flightDuration();
        }

        RandomSource random = RandomSource.create(seed);
        return 10 * flight + random.nextInt(6) + random.nextInt(7);
    }

    // https://prng.di.unimi.it/splitmix64.c
    private static long shotSeed(Player player, ItemStack fireworkItem, int shotIndex) {
        long seed = 0x9E3779B97F4A7C15L;
        seed ^= (long) player.getId() * 0xBF58476D1CE4E5B9L;
        seed ^= (long) shotIndex * 0x94D049BB133111EBL;
        seed ^= (long) ItemStack.hashItemAndComponents(fireworkItem) * 0xD6E8FEB86659FD93L;
        return seed;
    }

    private static long entitySeed(FireworkRocketEntity firework) {
        long seed = 0x9E3779B97F4A7C15L;
        seed ^= (long) firework.getId() * 0xBF58476D1CE4E5B9L;
        seed ^= (long) ItemStack.hashItemAndComponents(firework.getItem()) * 0xD6E8FEB86659FD93L;
        return seed;
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
