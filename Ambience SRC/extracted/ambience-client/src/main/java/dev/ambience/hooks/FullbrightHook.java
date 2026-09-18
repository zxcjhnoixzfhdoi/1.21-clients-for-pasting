package dev.ambience.hooks;

public final class FullbrightHook {
   private static volatile FullbrightHook.Impl impl;

   private FullbrightHook() {
   }

   public static void bind(FullbrightHook.Impl var0) {
      impl = var0;
   }

   public static boolean isGamma() {
      FullbrightHook.Impl var0 = impl;
      return var0 != null && var0.isGamma();
   }

   public interface Impl {
      boolean isGamma();
   }
}
