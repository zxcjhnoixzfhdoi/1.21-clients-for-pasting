package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.hacks.features.render.ESP;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private void krs$beginShaderEspItemCapture(PoseStack poseStack, SubmitNodeCollector submitter, int light, ArmedEntityRenderState state, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        ESP.beginItemCapture(state);
    }

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",
            at = @At("RETURN")
    )
    private void krs$endShaderEspItemCapture(PoseStack poseStack, SubmitNodeCollector submitter, int light, ArmedEntityRenderState state, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        ESP.endItemCapture();
    }
}
