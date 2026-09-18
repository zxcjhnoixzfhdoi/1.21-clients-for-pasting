package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.CustomSkyboxHook;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.MoonPhase;

@Mixin(SkyRendering.class)
public class MixinSkyRendererInject {
   @Inject(method = "updateRenderState", at = "RETURN", captureArgs = true)
   public static void customSky(SkyRendering var0, ClientWorld var1, float var2, Camera var3, SkyRenderState var4) {
      CustomSkyboxHook.apply(var4);
   }

   @Inject(method = "renderCelestialBodies", at = "HEAD", cancellable = true, captureArgs = true)
   public static void hideSunMoon(SkyRendering var0, MatrixStack var1, float var2, float var3, float var4, MoonPhase var5, float var6, float var7) {
      if (CustomSkyboxHook.hideSunMoon()) {
         Cancel.cancel();
      }
   }

   @Inject(method = "renderGlowingSky", at = "HEAD", cancellable = true, captureArgs = true)
   public static void hideSunrise(SkyRendering var0, MatrixStack var1, float var2, int var3) {
      if (CustomSkyboxHook.hideSunMoon()) {
         Cancel.cancel();
      }
   }
}
