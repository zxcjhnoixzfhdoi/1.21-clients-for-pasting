package onl.luka.grizzly.mixin.client;

import net.minecraft.client.gui.render.GuiRenderer;
import onl.luka.grizzly.util.HudBlur;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void grizzly$captureHudBlurFrame(CallbackInfo ci) {
        HudBlur.captureFrame();
    }

    @Inject(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V",
            shift = At.Shift.AFTER
        )
    )
    private void grizzly$refreshHudBlurAfterVanillaBlur(CallbackInfo ci) {
        HudBlur.refreshAfterVanillaBlur();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void grizzly$closeHudBlur(CallbackInfo ci) {
        HudBlur.close();
    }
}
