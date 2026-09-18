package hack.echo.client.utils.trajectory;

import hack.echo.client.utils.Imports;
import hack.echo.client.utils.simulation.WindChargeSimulation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

public final class WindChargeTrajectoryUtils implements Imports {

    private static final double WIND_CHARGE_SPEED = 1.5;

    private WindChargeTrajectoryUtils() {
    }

    public static List<Vec3> sample(Player player, float partialTick) {
        if (player == null || mc.level == null) return Collections.emptyList();

        Vec3 spawnPos = TrajectoryLaunchUtils.getProjectileSpawnPos(player, partialTick);
        Vec3 velocity = TrajectoryLaunchUtils.getLaunchDirection(player, partialTick)
            .scale(WIND_CHARGE_SPEED)
            .add(TrajectoryLaunchUtils.getInheritedVelocity(player));

        WindChargeSimulation simulation = new WindChargeSimulation(spawnPos, velocity, player);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::hasHit,
            simulation::simulateTick
        );
    }

    public static List<Vec3> sample(Vec3 position, Vec3 velocity) {
        if (position == null || velocity == null || mc.level == null) return Collections.emptyList();

        WindChargeSimulation simulation = new WindChargeSimulation(position, velocity, null);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::hasHit,
            simulation::simulateTick
        );
    }

    public static List<Vec3> sample(Entity windCharge) {
        if (windCharge == null || mc.level == null) return Collections.emptyList();

        WindChargeSimulation simulation = new WindChargeSimulation(windCharge.position(), windCharge.getDeltaMovement(), windCharge);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::hasHit,
            simulation::simulateTick
        );
    }
}
