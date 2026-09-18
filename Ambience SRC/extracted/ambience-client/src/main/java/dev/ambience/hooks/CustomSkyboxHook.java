package dev.ambience.hooks;

import net.minecraft.client.render.state.SkyRenderState;

public final class CustomSkyboxHook {
   private static volatile CustomSkyboxHook.Impl impl;

   private CustomSkyboxHook() {
   }

   public static void bind(CustomSkyboxHook.Impl var0) {
      impl = var0;
   }

   public static boolean enabled() {
      CustomSkyboxHook.Impl var0 = impl;
      return var0 != null && var0.enabled();
   }

   public static boolean hideSunMoon() {
      CustomSkyboxHook.Impl var0 = impl;
      return var0 != null && var0.enabled() && var0.hideSunMoon();
   }

   public static void apply(SkyRenderState var0) {
      CustomSkyboxHook.Impl var1 = impl;
      if (var1 != null && var1.enabled()) {
         var1.apply(var0);
      }
   }

   public interface Impl {
      boolean enabled();

      boolean hideSunMoon();

      void apply(SkyRenderState var1);
   }
}
