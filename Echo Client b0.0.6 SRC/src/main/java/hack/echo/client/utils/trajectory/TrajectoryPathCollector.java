package hack.echo.client.utils.trajectory;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class TrajectoryPathCollector {

    private TrajectoryPathCollector() {
    }

    public static List<Vec3> collect(int maxTicks, Supplier<Vec3> getPosition, BooleanSupplier isDone, Runnable simulateTick) {
        List<Vec3> points = new ArrayList<>(maxTicks + 1);
        points.add(getPosition.get());

        for (int i = 0; i < maxTicks && !isDone.getAsBoolean(); i++) {
            simulateTick.run();
            points.add(getPosition.get());
        }

        return points;
    }
}
