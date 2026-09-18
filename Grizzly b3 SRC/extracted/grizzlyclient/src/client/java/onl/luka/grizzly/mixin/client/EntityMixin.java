package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.combat.NoPush;
import onl.luka.grizzly.module.modules.movement.Flight;
import onl.luka.grizzly.module.modules.movement.Step;
import onl.luka.grizzly.util.RotationManager;
import onl.luka.grizzly.util.CameraOverriddenEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin implements CameraOverriddenEntity {
    @Unique
    private float cameraPitch;

    @Unique
    private float cameraYaw;

    @Unique
    private boolean turnCancelled = false;

    @Unique
    private boolean medved$trackingStep;

    @Unique
    private double medved$stepStartX;

    @Unique
    private double medved$stepStartY;

    @Unique
    private double medved$stepStartZ;

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 medved$modifyFlightMovement(Vec3 movement) {
        if ((Object) this instanceof LocalPlayer player) {
            return Flight.modifyMovement(player, movement);
        }
        return movement;
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void medved$beforeMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        this.medved$trackingStep = false;
        if (!((Object) this instanceof LocalPlayer player)
                || player != Minecraft.getInstance().player
                || !Step.shouldTrackMovement(player, movement)) {
            return;
        }

        this.medved$trackingStep = true;
        this.medved$stepStartX = player.getX();
        this.medved$stepStartY = player.getY();
        this.medved$stepStartZ = player.getZ();
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void medved$afterMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (!this.medved$trackingStep) return;
        this.medved$trackingStep = false;

        LocalPlayer player = (LocalPlayer) (Object) this;
        Step.onMovementComplete(
                player,
                this.medved$stepStartX,
                this.medved$stepStartY,
                this.medved$stepStartZ
        );
    }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void changeCameraLookDirection(double xDelta, double yDelta, CallbackInfo ci) {
        turnCancelled = false;
        if (RotationManager.perspective && (Object) this instanceof LocalPlayer) {
            double pitchDelta = (yDelta * 0.15);
            double yawDelta = (xDelta * 0.15);

            this.cameraPitch = Mth.clamp(this.cameraPitch + (float) pitchDelta, -90.0f, 90.0f);
            this.cameraYaw += (float) yawDelta;

            ci.cancel();
        }
    }

    @Inject(method = "turn", at = @At("RETURN"))
    private void medved$postTurn(double yaw, double pitch, CallbackInfo ci) {
        if (turnCancelled) {
            turnCancelled = false;
            return;
        }
        if ((Object) this instanceof LocalPlayer) {
            RotationManager.onTurn((LocalPlayer)(Object) this);
        }
    }


    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void noPush(Entity other, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if ((Object) this == mc.player && NoPush.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Override
    @Unique
    public float medved$getCameraPitch() {
        return this.cameraPitch;
    }

    @Override
    @Unique
    public float medved$getCameraYaw() {
        return this.cameraYaw;
    }

    @Override
    @Unique
    public void medved$setCameraPitch(float pitch) {
        this.cameraPitch = pitch;
    }

    @Override
    @Unique
    public void medved$setCameraYaw(float yaw) {
        this.cameraYaw = yaw;
    }
}
