package hack.echo.client.mixin.entity;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.movement.MoveFix;
import hack.echo.client.features.impl.render.Freecam;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.utils.player.PlayerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static hack.echo.client.Echo.featureManager;
import static hack.echo.client.utils.Imports.mc;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public abstract Vec3 calculateViewVector(float pitch, float yaw);


    @Inject(method = "getViewVector", at = @At("HEAD"), cancellable = true)
    private void injectSilentRotation(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this == mc.player) {
            if (RotationHandler.isHasSilentRotation()) {
                cir.setReturnValue(calculateViewVector(RotationHandler.getServerPitch(), RotationHandler.getServerYaw()));
            }
        }
    }

    @Inject(method = "getHeadLookAngle", at = @At("HEAD"), cancellable = true)
    private void injectSilentHeadRotation(CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this == mc.player) {
            if (RotationHandler.isHasSilentRotation()) {
                cir.setReturnValue(calculateViewVector(RotationHandler.getServerPitch(), RotationHandler.getServerYaw()));
            }
        }
    }

    @Redirect(
        method = "moveRelative",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getYRot()F"
        )
    )
    private float redirectMovementYaw(Entity instance) {
        if ((Object) this == mc.player && RotationHandler.isHasSilentRotation()) {
            if (Echo.isDestroyed) return instance.getYRot();
            if (featureManager == null) return instance.getYRot();
            MoveFix moveFix = featureManager.getFeatureByClass(MoveFix.class);
            if (moveFix == null || !moveFix.shouldApplyRotationFix()) return instance.getYRot();
            // Input correction (if MoveFix enabled) handles making movement go in intended direction
            return RotationHandler.getServerYaw();
        }
        return instance.getYRot();
    }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void echo$onTurn(double yaw, double pitch, CallbackInfo ci) {
        if ((Object)this != mc.player) return;
        if (Echo.isDestroyed) return;
        Freecam freecam = Echo.featureManager.getFeatureByClass(Freecam.class);
        if (freecam != null && freecam.isEnabled() && freecam.getPosition() != null) {
            freecam.rotate((float) yaw, (float) pitch);
            ci.cancel();
        }
    }

}
