package dev.ambience.hooks;

public final class TotemAnimationHook {
   private static volatile TotemAnimationHook.Impl impl;

   private TotemAnimationHook() {
   }

   public static void bind(TotemAnimationHook.Impl var0) {
      impl = var0;
   }

   public static boolean enabled() {
      TotemAnimationHook.Impl var0 = impl;
      return var0 != null && var0.enabled();
   }

   public static int animationLength() {
      TotemAnimationHook.Impl var0 = impl;
      return var0 == null ? 40 : var0.animationLength();
   }

   public static float animationScale() {
      TotemAnimationHook.Impl var0 = impl;
      return var0 == null ? 0.8F : var0.animationScale();
   }

   public interface Impl {
      boolean enabled();

      int animationLength();

      float animationScale();
   }
}
