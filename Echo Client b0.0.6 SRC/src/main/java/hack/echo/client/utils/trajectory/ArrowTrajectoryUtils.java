package hack.echo.client.utils.trajectory;

import hack.echo.client.utils.Imports;
import hack.echo.client.utils.simulation.ArrowSimulation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

public final class ArrowTrajectoryUtils implements Imports {

    private ArrowTrajectoryUtils() {
    }

    public static List<Vec3> sample(Vec3 position, Vec3 velocity) {
        if (position == null || velocity == null || mc.level == null) return Collections.emptyList();

        ArrowSimulation simulation = new ArrowSimulation(position, velocity, null);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::isInGround,
            simulation::simulateTick
        );
    }

    public static List<Vec3> sample(Entity arrow) {
        if (arrow == null || mc.level == null) return Collections.emptyList();

        ArrowSimulation simulation = new ArrowSimulation(arrow.position(), arrow.getDeltaMovement(), arrow);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::isInGround,
            simulation::simulateTick
        );
    }
}
