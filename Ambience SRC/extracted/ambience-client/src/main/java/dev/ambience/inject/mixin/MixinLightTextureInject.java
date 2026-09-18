package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.ambience.hooks.AntiBlindHook;
import dev.ambience.hooks.FullbrightHook;
import dev.ambience.hooks.XRayHook;
import dev.ambience.inject.Access;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;

@Mixin(LightmapTextureManager.class)
public class MixinLightTextureInject {
   @Inject(method = "update", at = "HEAD", cancellable = true, captureArgs = true)
   public static void fullbright(LightmapTextureManager var0, float var1) {
      if (FullbrightHook.isGamma() || XRayHook.active()) {
         GpuTexture var2 = Access.lightTexture(var0);
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(var2, -1);
         Cancel.cancel();
      }
   }

   @Inject(method = "getDarkness", at = "HEAD", cancellable = true, captureArgs = true)
   public static void antiBlind(LightmapTextureManager var0, LivingEntity var1, float var2, float var3) {
      if (AntiBlindHook.active()) {
         Cancel.cancel(0.0F);
      }
   }
}
