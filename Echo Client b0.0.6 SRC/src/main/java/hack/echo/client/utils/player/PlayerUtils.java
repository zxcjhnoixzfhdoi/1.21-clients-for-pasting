package hack.echo.client.utils.player;

import hack.echo.client.Echo;
import hack.echo.client.utils.Imports;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.utils.world.WorldUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class PlayerUtils implements Imports {

    public static Entity getClosestEnemy(float range) {
        return mc.level.players()
                .stream()
                .filter(player -> player != mc.player && mc.player.distanceTo(player) <= range && !player.isDeadOrDying() && player.isAlive())
                .min(Comparator.comparingDouble(mc.player::distanceTo))
                .orElse(null);
    }

    public static boolean isPlayerShieldingInOffHand() {
        if (mc.player == null) return false;
        return mc.player.isUsingItem()
                && mc.player.getOffhandItem().getItem() == Items.SHIELD
                && mc.player.getUsedItemHand() == InteractionHand.OFF_HAND;
    }

    public static boolean isPlayerShieldingInMainHand() {
        if (mc.player == null) return false;
        return mc.player.isUsingItem()
                && mc.player.getMainHandItem().getItem() == Items.SHIELD
                && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND;
    }

    public static boolean isMoving() {
        if (mc.player == null) return false;
        return mc.player.input.getMoveVector().length() != 0f;
    }

    // Returns what the server will see as fallDistance for this player this tick.
    // The server caps fallDistance at 1.0 when vertical speed > -0.5 (slow fall) and resets to 0 on upward motion.
    public static double getServerSyncedFallDistance(Player player) {
        if (player == null) return 0.0;
        double motionY = player.getDeltaMovement().y();
        double fd = player.fallDistance;
        if (motionY > 0.0) return 0.0;
        if (motionY > -0.5 && fd > 1.0) return 1.0;
        return fd;
    }

    public static double predictNextTickFallDistanceIfStopGliding() {
        if (mc.player == null) return 0.0;
        return predictNextTickFallDistanceIfStopGliding(mc.player);
    }

    public static void setMotionX(double motionX) {
        mc.player.setDeltaMovement(motionX, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z);
    }

    public static void setMotionY(double motionY) {
        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, motionY, mc.player.getDeltaMovement().z);
    }

    public static void setMotionZ(double motionZ) {
        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().y, motionZ);
    }

    public static double predictNextTickFallDistanceIfStopGliding(Player player) {
        if (player == null) return 0.0;

        double lastY = player.yo;
        double currentY = player.getY();
        double velocityY = player.getDeltaMovement().y;
        double currentFall = player.fallDistance;

        final double GRAVITY = 0.08;
        final double TERMINAL_VELOCITY = -3.92;
        final double STANDING_HITBOX_HEIGHT = 1.8;
        final double GLIDING_HITBOX_HEIGHT = 0.6;
        final double HITBOX_HEIGHT_DELTA = STANDING_HITBOX_HEIGHT - GLIDING_HITBOX_HEIGHT;

        double nextVelocityY = velocityY + GRAVITY;
        if (nextVelocityY < TERMINAL_VELOCITY) nextVelocityY = TERMINAL_VELOCITY;

        double predictedNextY = currentY + nextVelocityY;
        if (player.isFallFlying()) {
            predictedNextY += HITBOX_HEIGHT_DELTA;
        }
        double predictedIncrement = lastY - predictedNextY;

        double nextFall = currentFall + predictedIncrement;
        if (nextFall < 0) nextFall = 0.0;
        return nextFall;
    }

    public static LivingEntity getManualSelectTarget() {
        if (mc.player == null || mc.level == null) return null;

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float yaw = mc.player.getViewYRot(partialTick);
        float pitch = mc.player.getViewXRot(partialTick);
        Vec3 eyePos = mc.player.getEyePosition(partialTick);
        Vec3 lookDir = RotationHandler.getRotationVector(pitch, yaw);
        double range = mc.player.entityInteractionRange();
        Vec3 endPos = eyePos.add(lookDir.scale(range));

        AABB searchBox = mc.player.getBoundingBox().expandTowards(lookDir.scale(range)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player, eyePos, endPos, searchBox,
                entity -> !entity.isSpectator() && entity.isPickable(),
                range * range);

        if (entityHit == null) return null;
        if (!(entityHit.getEntity() instanceof LivingEntity living)) return null;
        return living;
    }

    public static Boolean isCollidingWithHitbox(LivingEntity player, LivingEntity entity) {
        if (player == null || entity == null) return false;
        return player.getBoundingBox().intersects(entity.getBoundingBox());
    }

    public static Boolean isCollidingWithHitbox(LivingEntity player) {
        if (player == null) return false;
        if (mc.level == null) return false;
        
        return mc.level.getEntities(player, player.getBoundingBox())
                .stream()
                .anyMatch(entity -> entity instanceof LivingEntity && entity != player);
    }

    public static boolean isFriend(String name) {
        if (Echo.friendManager == null) {
            return false;
        }

        return Echo.friendManager.isFriend(name);
    }
}
