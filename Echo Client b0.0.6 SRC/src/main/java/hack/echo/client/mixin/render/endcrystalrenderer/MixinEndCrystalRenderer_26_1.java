package hack.echo.client.mixin.render.endcrystalrenderer;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import hack.echo.client.Echo;
import hack.echo.client.features.impl.render.NoRender;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalRenderer.class)
public class MixinEndCrystalRenderer_26_1 {

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    public void echo$render(EndCrystalRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera, CallbackInfo ci) {
        if (Echo.isDestroyed) return;
        if (Echo.featureManager == null) return;
        NoRender noRender = Echo.featureManager.getFeatureByClass(NoRender.class);
        if (noRender != null && noRender.isEnabled() && noRender.noCrystalBounce.getValue()) {
            state.ageInTicks = 0f;
        }
    }
}
*///?} else {
public final class MixinEndCrystalRenderer_26_1 {
}
//?}
