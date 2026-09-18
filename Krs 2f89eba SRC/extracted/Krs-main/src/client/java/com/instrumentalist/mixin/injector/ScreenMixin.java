package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.utils.GuiInputBlocker;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int blockRenderMouseXWhenImguiCaptures(int mouseX) {
        return GuiInputBlocker.sanitizeMouseCoordinate(mouseX);
    }

    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int blockRenderMouseYWhenImguiCaptures(int mouseY) {
        return GuiInputBlocker.sanitizeMouseCoordinate(mouseY);
    }

    @Inject(method = "isMouseOver(DD)Z", at = @At("HEAD"), cancellable = true)
    private void blockScreenHoverWhenImguiCaptures(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (GuiInputBlocker.shouldBlockMinecraftMouse())
            cir.setReturnValue(false);
    }
}
