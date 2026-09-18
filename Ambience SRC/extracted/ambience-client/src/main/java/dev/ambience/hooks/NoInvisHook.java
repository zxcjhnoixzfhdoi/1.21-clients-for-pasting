package dev.ambience.hooks;

public final class NoInvisHook {
   private static volatile NoInvisHook.Impl impl;

   private NoInvisHook() {
   }

   public static void bind(NoInvisHook.Impl var0) {
      impl = var0;
   }

   public static boolean enabled() {
      NoInvisHook.Impl var0 = impl;
      return var0 != null && var0.enabled();
   }

   public interface Impl {
      boolean enabled();
   }
}
