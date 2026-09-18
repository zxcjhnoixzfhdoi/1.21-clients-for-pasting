package we.devs.opium.api.utilities;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import static we.devs.opium.Opium.mc;

public class CxRotations {

    public enum Rotate {
        None,
        Default,
        Grim
    }

    public static void rotateToBlock(BlockPos bp, Rotate rotate, boolean ignoreEntities) {
        BlockHitResult result = CxBlockUtil.getPlaceResult(bp, ignoreEntities);
        if (result == null || mc.player == null) return;

        float[] angle = calculateAngle(result.getPos());

        switch (rotate) {
            case None -> {
                // No rotation needed
            }
            case Default -> mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(angle[0], angle[1], mc.player.isOnGround()));
            case Grim -> mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), angle[0], angle[1], mc.player.isOnGround()));
            default -> throw new IllegalStateException("Unexpected value: " + rotate);
        }

        if (rotate == Rotate.Grim) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
        }
    }

    public static float @NotNull [] calculateAngle(Vec3d to) {
        assert mc.player != null;
        return calculateAngle(CxBlockUtil.getEyesPos(mc.player), to);
    }

    public static float @NotNull [] calculateAngle(@NotNull Vec3d from, @NotNull Vec3d to) {
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * -1.0;
        double difZ = to.z - from.z;
        double dist = MathHelper.sqrt((float) (difX * difX + difZ * difZ));

        float yD = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
        float pD = (float) MathHelper.clamp(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist))), -90f, 90f);

        return new float[]{yD, pD};
    }
}