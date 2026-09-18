package hack.echo.client.mixin.client;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventSetPerspective;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinOptions {

    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    private void echo$onSetCameraType(CameraType cameraType, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        
        EventSetPerspective event = new EventSetPerspective(cameraType);
        event.call();
        
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
