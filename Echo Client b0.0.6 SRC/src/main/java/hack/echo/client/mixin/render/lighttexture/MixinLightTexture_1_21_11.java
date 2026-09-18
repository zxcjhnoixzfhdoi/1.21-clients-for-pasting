package hack.echo.client.mixin.render.lighttexture;

//? if <26.1 {
import hack.echo.client.Echo;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import hack.echo.client.features.impl.render.FullbrightModule;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class MixinLightTexture_1_21_11 {

    @Shadow @Final private GpuTexture texture;

    @Inject(method = "updateLightTexture", at = @At("HEAD"), cancellable = true)
    private void echo$renderFullbrightLightTexture(float tickProgress, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (!FullbrightModule.isFullbrightEnabled()) {
            return;
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.texture, -1);
        ci.cancel();
    }
}
//?} else {
/*public final class MixinLightTexture_1_21_11 {
}
*///?}
