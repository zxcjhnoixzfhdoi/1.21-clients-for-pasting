package we.devs.opium.api.utilities;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;

import static we.devs.opium.Opium.mc;

public class RotationsUtil {

    /**
     * Interpolates the yaw value towards a target yaw using a given step.
     */
    public static float nextYaw(double current, double target, double step) {
        double deltaYaw = yawAngle(current, target);
        return (float) (Math.abs(deltaYaw) <= step ? target : current + Math.copySign(step, deltaYaw));
    }

    /**
     * Calculates the shortest yaw angle between two yaw values.
     */
    public static double yawAngle(double current, double target) {
        return MathHelper.wrapDegrees(target - current);
    }

    /**
     * Interpolates the pitch value towards a target pitch using a given step.
     */
    public static float nextPitch(double current, double target, double step) {
        double deltaPitch = target - current;
        return (float) (Math.abs(deltaPitch) <= step ? target : current + Math.copySign(step, deltaPitch));
    }

    /**
     * Calculates the angle (in radians) between two 2D vectors.
     */
    public static double radAngle(Vec2f vec1, Vec2f vec2) {
        double dotProduct = vec1.x * vec2.x + vec1.y * vec2.y;
        double magnitude1 = Math.sqrt(vec1.x * vec1.x + vec1.y * vec1.y);
        double magnitude2 = Math.sqrt(vec2.x * vec2.x + vec2.y * vec2.y);
        return Math.acos(dotProduct / (magnitude1 * magnitude2));
    }

    /**
     * Gets the yaw required to face a target position from a starting position.
     */
    public static double getYaw(Vec3d start, Vec3d target) {
        return MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(target.getZ() - start.getZ(), target.getX() - start.getX())) - 90);
    }

    /**
     * Gets the pitch required to face a target position from a starting position.
     */
    public static double getPitch(Vec3d start, Vec3d target) {
        double diffX = target.getX() - start.getX();
        double diffY = target.getY() - (start.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = target.getZ() - start.getZ();
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        return MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
    }

    /**
     * Gets the yaw required to face a BlockPos.
     */
    public static double getYaw(BlockPos pos) {
        Vec3d targetCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return getYaw(mc.player.getPos(), targetCenter);
    }

    /**
     * Gets the pitch required to face a BlockPos.
     */
    public static double getPitch(BlockPos pos) {
        Vec3d targetCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return getPitch(mc.player.getPos(), targetCenter);
    }

    /**
     * Rotates the camera to a specific yaw and pitch.
     */
    public static void setCamRotation(double yaw, double pitch) {
        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }

    /**
     * Sends a rotation packet to the server for the player.
     */
    public static void sendRotationPacket(double yaw, double pitch) {
        mc.getNetworkHandler().sendPacket(
                new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround((float) yaw, (float) pitch, mc.player.isOnGround())
        );
    }

    /**
     * Calculates a rotation (yaw and pitch) to face a target position.
     */
    public static Vec2f calculateRotation(Vec3d target) {
        double yaw = getYaw(mc.player.getPos(), target);
        double pitch = getPitch(mc.player.getPos(), target);
        return new Vec2f((float) yaw, (float) pitch);
    }

    /**
     * Calculates a rotation (yaw and pitch) to face a BlockPos.
     */
    public static Vec2f calculateRotation(BlockPos pos) {
        return calculateRotation(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    /**
     * Rotates the player to face a BlockPos.
     *
     * @param pos           The target block position.
     * @param clientRotation If true, adjusts the client's camera rotation; otherwise, only sends a rotation packet.
     */
    public static void rotateToBlockPos(BlockPos pos, boolean clientRotation) {
        Vec2f rotation = calculateRotation(pos);
        if (clientRotation) {
            setCamRotation(rotation.x, rotation.y);
        }
        sendRotationPacket(rotation.x, rotation.y);
    }
}
