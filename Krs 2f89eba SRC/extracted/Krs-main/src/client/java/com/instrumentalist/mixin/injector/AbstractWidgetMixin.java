package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.utils.GuiInputBlocker;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {

    @Inject(method = "isMouseOver(DD)Z", at = @At("HEAD"), cancellable = true)
    private void blockHoverWhenImguiCaptures(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (GuiInputBlocker.shouldBlockMinecraftMouse())
            cir.setReturnValue(false);
    }
}
