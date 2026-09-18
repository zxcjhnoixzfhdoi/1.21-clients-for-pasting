package we.devs.opium.asm.mixins;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import we.devs.opium.api.manager.module.ModuleManager;
import we.devs.opium.client.modules.visuals.ModuleCameraClip;

@Mixin(Camera.class)
public abstract class CameraMixin {


    @Shadow protected abstract float clipToSpace(float f);

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    private void modifyCameraDistance(Args args) {
        if (ModuleCameraClip.INSTANCE.isToggled()) {
            args.set(0, -clipToSpace(ModuleCameraClip.INSTANCE.getDistance()));
        }
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(float f, CallbackInfoReturnable<Float> cir) {
        if (ModuleCameraClip.INSTANCE.isToggled()) {
            cir.setReturnValue(ModuleCameraClip.INSTANCE.getDistance());
        }
    }

}
