package dev.ambience.hooks;

public final class LowFireHook {
   private static volatile LowFireHook.Impl impl;

   private LowFireHook() {
   }

   public static void bind(LowFireHook.Impl var0) {
      impl = var0;
   }

   public static boolean enabled() {
      LowFireHook.Impl var0 = impl;
      return var0 != null && var0.enabled();
   }

   public static float fireYOffset() {
      LowFireHook.Impl var0 = impl;
      return var0 == null ? 0.0F : var0.fireYOffset();
   }

   public static float fireHeightScale() {
      LowFireHook.Impl var0 = impl;
      return var0 == null ? 1.0F : var0.fireHeightScale();
   }

   public static float fireUvSlice(float var0, float var1) {
      LowFireHook.Impl var2 = impl;
      return var2 == null ? var1 - var0 : var2.fireUvSlice(var0, var1);
   }

   public interface Impl {
      boolean enabled();

      float fireYOffset();

      float fireHeightScale();

      float fireUvSlice(float var1, float var2);
   }
}
