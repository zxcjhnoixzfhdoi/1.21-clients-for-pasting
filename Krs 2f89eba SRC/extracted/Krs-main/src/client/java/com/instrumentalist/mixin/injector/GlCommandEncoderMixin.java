package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.hacks.features.level.CaveFinder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderMixin {

    @Inject(
            method = "applyPipelineState(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V",
            at = @At("RETURN")
    )
    private void krs$caveFinderCullFace(RenderPipeline pipeline, CallbackInfo ci) {
        GL11.glCullFace(CaveFinder.shouldCullFront(pipeline) ? GL11.GL_FRONT : GL11.GL_BACK);
    }
}
