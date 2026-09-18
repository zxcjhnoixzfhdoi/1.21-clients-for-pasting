package hack.echo.client.utils.player;

import hack.echo.client.utils.Imports;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class CustomFallDistance implements Imports {

    private static double customFallDistance = 0.0;
    private static double lastY = 0.0;
    private static boolean wasOnGround = false;
    private static boolean wasInWater = false;
    private static boolean wasGliding = false;
    private static int ticksSinceGround = 0;

    private static final double LAVA_REDUCTION_FACTOR = 0.5;
    private static final double MIN_FALL_THRESHOLD = 0.001;

    public static void update() {
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }

        Player player = mc.player;
        double currentY = player.getY();
        boolean onGround = player.onGround();
        boolean inWater = player.isInWater();
        boolean inLava = player.isInLava();
        boolean gliding = player.isFallFlying();
        boolean inVehicle = player.isPassenger();

        if (onGround && !wasOnGround) {
            reset();
            wasOnGround = true;
            ticksSinceGround = 0;
            return;
        }

        if (inWater && !wasInWater) {
            reset();
            wasInWater = true;
            return;
        }

        if (gliding && !wasGliding) {
            reset();
            wasGliding = true;
            return;
        }

        if (inVehicle) {
            customFallDistance = player.fallDistance;
            lastY = currentY;
            return;
        }

        BlockPos blockBelow = player.blockPosition().below();
        if (mc.level.getBlockState(blockBelow).getBlock().toString().toLowerCase().contains("honey")) {
            reset();
            return;
        }

        double yDiff = lastY - currentY;

        if (!onGround && !inWater && !gliding) {
            Vec3 velocity = player.getDeltaMovement();

            if (velocity.y > 0.0) {
                customFallDistance = 0.0;
            } else if (velocity.y < 0 || yDiff > MIN_FALL_THRESHOLD) {
                customFallDistance += yDiff;

                if (inLava) {
                    customFallDistance *= LAVA_REDUCTION_FACTOR;
                }

                if (customFallDistance < 0) {
                    customFallDistance = 0;
                }

                if (velocity.y > -0.5 && customFallDistance > 1.0) {
                    customFallDistance = 1.0;
                }

                ticksSinceGround++;
            } else if (velocity.y > 0.42) {
                customFallDistance = Math.max(0, customFallDistance - Math.abs(yDiff));
            }
        }

        lastY = currentY;
        wasOnGround = onGround;
        wasInWater = inWater;
        wasGliding = gliding;
    }

    public static double get() {
        update();
        return customFallDistance;
    }

    public static double getVanilla() {
        return mc.player != null ? mc.player.fallDistance : 0.0;
    }

    public static double getDifference() {
        return get() - getVanilla();
    }

    public static int getTicksSinceGround() {
        return ticksSinceGround;
    }

    public static void reset() {
        customFallDistance = 0.0;
        ticksSinceGround = 0;
        if (mc.player != null) {
            lastY = mc.player.getY();
            wasOnGround = mc.player.onGround();
            wasInWater = mc.player.isInWater();
            wasGliding = mc.player.isFallFlying();
        }
    }

    public static void set(double distance) {
        customFallDistance = Math.max(0, distance);
    }

    public static boolean canCrit() {
        if (mc.player == null) return false;
        Vec3 v = CustomGetVelocity.get(mc.player);
        final double EPS = 1.0e-4;
        return !mc.player.onGround()
            && !mc.player.isPassenger()
            && !mc.player.onClimbable()
            && !mc.player.isInWater()
            && !mc.player.isFallFlying()
            && customFallDistance > 0.0
            && v.y < -EPS;
    }

    public static String getFormatted() {
        return String.format("%.2f blocks", customFallDistance);
    }

    public static boolean isTracking() {
        if (mc.player == null) return false;
        return !mc.player.onGround()
            && !mc.player.isInWater()
            && !mc.player.isFallFlying()
            && !mc.player.isPassenger();
    }
}
