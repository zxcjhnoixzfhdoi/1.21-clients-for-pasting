package we.devs.opium.asm.mixins;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import we.devs.opium.api.utilities.RenderUtils;

@Mixin(value = SplashOverlay.class, priority = 3001)
public abstract class MixinSplashScreen {
    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RenderUtils.setDrawContext(context);
    }
}
