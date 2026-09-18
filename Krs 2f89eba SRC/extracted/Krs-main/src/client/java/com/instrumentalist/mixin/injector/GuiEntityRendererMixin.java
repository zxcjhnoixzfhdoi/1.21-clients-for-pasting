package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.utils.render.GuiEntityRenderGuard;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiEntityRenderer.class)
public abstract class GuiEntityRendererMixin {

    @Inject(
            method = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            at = @At("HEAD")
    )
    private void krs$beginGuiEntityRender(GuiEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitter, CallbackInfo ci) {
        GuiEntityRenderGuard.begin();
    }

    @Inject(
            method = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            at = @At("RETURN")
    )
    private void krs$endGuiEntityRender(GuiEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitter, CallbackInfo ci) {
        GuiEntityRenderGuard.end();
    }
}
