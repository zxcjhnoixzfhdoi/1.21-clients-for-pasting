package hack.echo.client.utils.trajectory;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TrajectoryLaunchUtils {

    private static final double PROJECTILE_SPAWN_Y_OFFSET = 0.1;

    private TrajectoryLaunchUtils() {
    }

    public static Vec3 getProjectileSpawnPos(Player player, float partialTick) {
        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight() - PROJECTILE_SPAWN_Y_OFFSET;
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        return new Vec3(x, y, z);
    }

    public static Vec3 getLaunchDirection(Player player, float partialTick) {
        return Vec3.directionFromRotation(player.getViewXRot(partialTick), player.getViewYRot(partialTick)).normalize();
    }

    public static Vec3 getLaunchDirection(Player player, float pitchOffset, float partialTick) {
        return Vec3.directionFromRotation(player.getViewXRot(partialTick) + pitchOffset, player.getViewYRot(partialTick)).normalize();
    }

    public static Vec3 getInheritedVelocity(Player player) {
        Vec3 movement = player.getDeltaMovement();
        return player.onGround() ? new Vec3(movement.x, 0.0, movement.z) : movement;
    }

}
