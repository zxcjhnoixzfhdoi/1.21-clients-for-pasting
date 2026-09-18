package hack.echo.client.features.impl.macros;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.*;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.utils.rotation.RotationUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Tunneler extends Feature {

    public Tunneler() {
        super(new FeatureInfo(
                Concat.of("Tunneler"),
                Concat.of("Automatically mines tunnels while walking forward"),
                Category.MACROS,
                false
        ));
    }

    private final ModeSetting mode = new
            ModeSetting(Concat.of("Mode"), Concat.of("2x1"), Concat.of("2x1"), Concat.of("1x1"));
    private final BoolSetting silent = new BoolSetting(Concat.of("Silent"), true);

    private boolean mining = false;
    private boolean aimingUpper = true; // For 2x1 mode: alternates between upper and lower block
    private boolean wasTracking = false;

    @EventSubscribe
    private void onTick(EventTick e) {
        if (isNull()) return;

        if (mc.hitResult instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            Direction dir = bhr.getDirection();

            if (mc.level.getBlockState(pos).isAir()) {
                mining = false;
                return;
            }

            if (mc.gameMode != null) {
                if (mc.gameMode.continueDestroyBlock(pos, dir)) {
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    mining = true;
                } else {
                    mining = false;
                }
            }
        }
    }

    @EventSubscribe
    private void onMoveInput(EventMovementInput e) {
        if (isNull()) return;
        e.forward = true;
        e.back = false;
        e.left = false;
        e.right = false;
        e.jump = false;
        e.sneak = false;
        e.forceMovement = true;
    }

    @EventSubscribe
    private void onSilentAim(EventMove.Pre e) {
        if (isNull()) return;
        if (!RotationUtils.hasSilentRotation()) return;

        e.setYaw(RotationUtils.getSilentYaw());
        e.setPitch(RotationUtils.getSilentPitch());
    }

    @EventSubscribe
    private void onAim(MouseUpdateEvent e) {
        if (isNull()) return;

        Vec3 target = getTargetPosition();

        if (target == null) {
            if (wasTracking && RotationUtils.isTracking() && RotationUtils.isControlledBy(this)) {
                if (!RotationUtils.rotateBack(true)) {
                    wasTracking = false;
                }
            } else if (RotationUtils.isControlledBy(this)) {
                RotationUtils.stopTracking();
                wasTracking = false;
            }
            return;
        }

        boolean success = RotationUtils.aim(this)
                .silent(silent.getValue())
                .speed(100f)
                .priority(EventSubscribe.Priority.HIGH)
                .to(target);

        if (success) {
            wasTracking = true;
        }
    }

    private Vec3 getTargetPosition() {
        if (mc.player == null || mc.level == null) return null;

        float yaw = mc.player.getYRot();
        double yawRad = Math.toRadians(yaw);

        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        Vec3 playerPos = mc.player.position();
        int feetBlockY = (int) Math.floor(playerPos.y);

        double targetX = playerPos.x + forwardX;
        double targetZ = playerPos.z + forwardZ;

        if (mode.is(Concat.of("1x1"))) {
            BlockPos targetPos = new BlockPos((int) Math.floor(targetX), feetBlockY, (int) Math.floor(targetZ));

            if (mc.level.getBlockState(targetPos).isAir()) {
                return null;
            }

            return Vec3.atCenterOf(targetPos);
        } else {
            BlockPos lowerPos = new BlockPos((int) Math.floor(targetX), feetBlockY, (int) Math.floor(targetZ));
            BlockPos upperPos = new BlockPos((int) Math.floor(targetX), feetBlockY + 1, (int) Math.floor(targetZ));

            boolean upperSolid = !mc.level.getBlockState(upperPos).isAir();
            boolean lowerSolid = !mc.level.getBlockState(lowerPos).isAir();

            if (!upperSolid && !lowerSolid) {
                return null;
            }

            if (upperSolid && !lowerSolid) {
                aimingUpper = true;
            } else if (!upperSolid && lowerSolid) {
                aimingUpper = false;
            } else {
                if (!mining) {
                    aimingUpper = !aimingUpper;
                }
            }

            BlockPos targetPos = aimingUpper ? upperPos : lowerPos;
            return Vec3.atCenterOf(targetPos);
        }
    }

    @EventSubscribe
    private void onAttack(EventStartAttack.Pre e) {
        e.cancel();
    }

    @EventSubscribe
    private void onAttack(EventBlockBreaking.Pre e) {
        e.cancel();
    }

    @Override
    public void onDisable() {
        if (RotationUtils.isControlledBy(this)) {
            RotationUtils.stopTracking();
        }
        wasTracking = false;
        mining = false;
        aimingUpper = true;
        super.onDisable();
    }
}
