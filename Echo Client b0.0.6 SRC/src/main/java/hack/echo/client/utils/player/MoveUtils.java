package hack.echo.client.utils.player;

import hack.echo.client.utils.Imports;
import lombok.experimental.UtilityClass;
import net.minecraft.world.entity.player.Input;
import net.minecraft.util.Mth;

import static hack.echo.client.utils.player.PlayerUtils.isMoving;

@UtilityClass
public class MoveUtils implements Imports {

    public static double getDirection() {
        float rotationYaw = mc.player.getYRot();

        if (Input.EMPTY.forward()) {
            rotationYaw += 0F;
        } else if (Input.EMPTY.backward()) {
            rotationYaw += 180F;
        }

        float forward = 1F;

        if (Input.EMPTY.backward()) {
            forward = -0.5F;
        } else if (Input.EMPTY.forward()) {
            forward = 0.5F;
        }

        if (Input.EMPTY.left()) {
            rotationYaw += 90F * forward;
        }
        if (Input.EMPTY.right()) {
            rotationYaw -= 90F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static void strafe(final float speed) {
        if (!isMoving()) {
            return;
        }

        final double yaw = getDirection();
        mc.player.setDeltaMovement(-Mth.sin((float) yaw) * speed, mc.player.getDeltaMovement().y, Mth.cos((float) yaw) * speed);
    }

    public static void motion(final float speed) {
        if (!isMoving()) {
            return;
        }

        final double yaw = getDirection();
        mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().x - Math.sin(yaw) * speed,
                mc.player.getDeltaMovement().y,
                mc.player.getDeltaMovement().z + Math.cos(yaw) * speed
        );
    }
}
