package hack.echo.client.utils.trajectory;

import hack.echo.client.utils.Imports;
import hack.echo.client.utils.simulation.PotionSimulation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

public final class PotionTrajectoryUtils implements Imports {

    private static final float POTION_PITCH_OFFSET = -20.0F;
    private static final double POTION_SPEED = 0.5;

    private PotionTrajectoryUtils() {
    }

    public static List<Vec3> sample(Player player, float partialTick) {
        if (player == null || mc.level == null) return Collections.emptyList();

        Vec3 spawnPos = TrajectoryLaunchUtils.getProjectileSpawnPos(player, partialTick);
        Vec3 velocity = TrajectoryLaunchUtils.getLaunchDirection(player, POTION_PITCH_OFFSET, partialTick)
            .scale(POTION_SPEED)
            .add(TrajectoryLaunchUtils.getInheritedVelocity(player));

        PotionSimulation simulation = new PotionSimulation(spawnPos, velocity, player);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::hasHit,
            simulation::simulateTick
        );
    }

    public static List<Vec3> sample(Vec3 position, Vec3 velocity) {
        if (position == null || velocity == null || mc.level == null) return Collections.emptyList();

        PotionSimulation simulation = new PotionSimulation(position, velocity, null);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::hasHit,
            simulation::simulateTick
        );
    }

    public static List<Vec3> sample(Entity potion) {
        if (potion == null || mc.level == null) return Collections.emptyList();

        PotionSimulation simulation = new PotionSimulation(potion.position(), potion.getDeltaMovement(), potion);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::hasHit,
            simulation::simulateTick
        );
    }
}
