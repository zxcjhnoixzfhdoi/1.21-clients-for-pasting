package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.utils.IMinecraft;
import com.instrumentalist.krs.Client;
import com.instrumentalist.krs.screen.CustomTitleScreen;
import com.instrumentalist.krs.screen.NanoVGClickGuiScreen;
import com.instrumentalist.krs.utils.nanovg.NanoVGManager;
import com.instrumentalist.krs.utils.render.DebugOverlayRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.breadloaf.imguimc.imgui.ImguiLoader;

@Mixin(Minecraft.class)
public abstract class RenderSystemMixin implements IMinecraft {

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuSurface;present()V"
            )
    )
    private void runTickTail(boolean renderLevel, CallbackInfo ci) {
        if (Client.nanoVgManager != null) {
            NanoVGClickGuiScreen.renderDetachedIfNeeded();
            Client.nanoVgManager.renderQueued();
            CustomTitleScreen.renderStartupLoadBarIfNeeded(Client.nanoVgManager);
        }

        if (NanoVGManager.shouldRenderBelowDebugOverlay() && mc.gameRenderer instanceof DebugOverlayRenderer debugOverlayRenderer) {
            debugOverlayRenderer.krs$renderDebugOverlayOnTop();
        }

        ImguiLoader.onFrameRender();
    }
}
