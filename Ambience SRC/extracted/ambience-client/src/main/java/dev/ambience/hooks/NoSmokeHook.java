package dev.ambience.hooks;

import net.minecraft.particle.ParticleEffect;

public final class NoSmokeHook {
   private static volatile NoSmokeHook.Impl impl;

   private NoSmokeHook() {
   }

   public static void bind(NoSmokeHook.Impl var0) {
      impl = var0;
   }

   public static boolean enabled() {
      NoSmokeHook.Impl var0 = impl;
      return var0 != null && var0.enabled();
   }

   public static boolean shouldCancel(ParticleEffect var0) {
      NoSmokeHook.Impl var1 = impl;
      return var1 != null && var1.shouldCancel(var0);
   }

   public interface Impl {
      boolean enabled();

      boolean shouldCancel(ParticleEffect var1);
   }
}
