package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.Client;
import com.instrumentalist.krs.hacks.features.render.Rotations;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @ModifyExpressionValue(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;solveBodyRot(Lnet/minecraft/world/entity/LivingEntity;FF)F")
    )
    private float krs$vanillaRotationBodyYaw(float original, LivingEntity entity, LivingEntityRenderState state, float tickDelta) {
        if (krs$shouldApplyVanillaRotation(entity))
            return Client.rotationManager.getInterpolatedBodyYaw(tickDelta);

        return original;
    }

    @ModifyExpressionValue(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F", ordinal = 0)
    )
    private float krs$vanillaRotationHeadYaw(float original, LivingEntity entity, LivingEntityRenderState state, float tickDelta) {
        if (krs$shouldApplyVanillaRotation(entity))
            return Client.rotationManager.getInterpolatedYaw(tickDelta);

        return original;
    }

    @ModifyExpressionValue(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F")
    )
    private float krs$vanillaRotationPitch(float original, LivingEntity entity, LivingEntityRenderState state, float tickDelta) {
        if (krs$shouldApplyVanillaRotation(entity))
            return Client.rotationManager.getInterpolatedPitch(tickDelta);

        return original;
    }

    @Unique
    private boolean krs$shouldApplyVanillaRotation(LivingEntity entity) {
        return Client.rotationManager != null
                && Client.rotationManager.isRotating()
                && Rotations.shouldUseVanilla()
                && entity == Minecraft.getInstance().player;
    }
}
