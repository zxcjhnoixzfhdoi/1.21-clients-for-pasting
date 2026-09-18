package hack.echo.client.mixin.render.lightmap;

//? if >=26.1 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.FullbrightModule;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lightmap.class)
public class MixinLightmap_26_1 {

    @Shadow
    @Final
    private GpuTexture texture;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void echo$renderFullbrightLightmap(LightmapRenderState renderState, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        if (!FullbrightModule.isFullbrightEnabled()) {
            return;
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.texture, -1);
        ci.cancel();
    }
}
*///?} else {
public final class MixinLightmap_26_1 {
}
//?}
