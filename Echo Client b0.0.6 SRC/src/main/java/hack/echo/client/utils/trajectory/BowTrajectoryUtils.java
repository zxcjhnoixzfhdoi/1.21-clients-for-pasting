package hack.echo.client.utils.trajectory;

import hack.echo.client.utils.Imports;
import hack.echo.client.utils.simulation.BowSimulation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

public final class BowTrajectoryUtils implements Imports {

    private BowTrajectoryUtils() {
    }

    public static List<Vec3> sample(Player player, int chargeTicks, float partialTick) {
        if (player == null || mc.level == null) return Collections.emptyList();

        BowSimulation simulation = new BowSimulation(player, Math.max(0, chargeTicks), partialTick);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::hasHit,
            simulation::simulateTick
        );
    }
}
