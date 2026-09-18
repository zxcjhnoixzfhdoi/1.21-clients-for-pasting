package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.hacks.features.render.ESP;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SubmitNodeStorage.class)
public abstract class SubmitNodeStorageMixin {

    @Inject(method = "submitModel", at = @At("HEAD"))
    private <S> void krs$captureShaderEspSubmittedModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int color, TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        ESP.captureSubmittedModel(model, state, poseStack, renderType, lightCoords, overlayCoords);
    }

    @Inject(method = "submitItem", at = @At("HEAD"))
    private void krs$captureShaderEspItem(PoseStack poseStack, ItemDisplayContext displayContext, int light, int overlay, int color, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType, CallbackInfo ci) {
        ESP.captureItem(poseStack, quads);
    }
}
