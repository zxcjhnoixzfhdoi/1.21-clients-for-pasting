package dev.ambience.mixin.client;

import dev.ambience.hooks.CustomSkyboxHook;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class MixinSkyRenderer {
   @Inject(method = "updateRenderState", at = @At("RETURN"))
   private void ambience$customSky(ClientWorld var1, float var2, Camera var3, SkyRenderState var4, CallbackInfo var5) {
      CustomSkyboxHook.apply(var4);
   }

   @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true)
   private void ambience$hideSunMoon(MatrixStack var1, float var2, float var3, float var4, MoonPhase var5, float var6, float var7, CallbackInfo var8) {
      if (CustomSkyboxHook.hideSunMoon()) {
         var8.cancel();
      }
   }

   @Inject(method = "renderGlowingSky", at = @At("HEAD"), cancellable = true)
   private void ambience$hideSunrise(MatrixStack var1, float var2, int var3, CallbackInfo var4) {
      if (CustomSkyboxHook.hideSunMoon()) {
         var4.cancel();
      }
   }
}
