package hack.echo.client.mixin.render.screeneffectrenderer;

//? if >26.1.2 {
/*import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventRenderFireOverlay;
import hack.echo.client.event.impl.EventRenderFloatingItem;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class MixinScreenEffectRenderer_26_2 {

    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void echo$renderFireOverlay(PoseStack matrices, SubmitNodeCollector vertexConsumers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderFireOverlay event = new EventRenderFireOverlay();
        event.call();

        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
    public void echo$renderFloatingItem(PoseStack matrices, float tickProgress, SubmitNodeCollector queue, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventRenderFloatingItem event = new EventRenderFloatingItem();
        event.call();

        if (event.cancelled) {
            ci.cancel();
        }
    }
}
*///?} else {
public final class MixinScreenEffectRenderer_26_2 {
}
//?}
