package dev.ambience.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.ambience.hooks.AntiBlindHook;
import dev.ambience.hooks.FullbrightHook;
import dev.ambience.hooks.XRayHook;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public class MixinLightTexture {
   @Shadow
   @Final
   private GpuTexture glTexture;

   @Inject(method = "update", at = @At("HEAD"), cancellable = true)
   private void ambience$fullbright(float var1, CallbackInfo var2) {
      if (FullbrightHook.isGamma() || XRayHook.active()) {
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.glTexture, -1);
         var2.cancel();
      }
   }

   @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
   private void ambience$antiBlind(LivingEntity var1, float var2, float var3, CallbackInfoReturnable<Float> var4) {
      if (AntiBlindHook.active()) {
         var4.setReturnValue(0.0F);
      }
   }
}
