package dev.ambience.hooks;

public final class FreelookHook {
   private static volatile FreelookHook.Impl impl;

   private FreelookHook() {
   }

   public static void bind(FreelookHook.Impl var0) {
      impl = var0;
   }

   public static boolean isActive() {
      FreelookHook.Impl var0 = impl;
      return var0 != null && var0.isActive();
   }

   public static float getYaw() {
      FreelookHook.Impl var0 = impl;
      return var0 == null ? 0.0F : var0.getYaw();
   }

   public static float getPitch() {
      FreelookHook.Impl var0 = impl;
      return var0 == null ? 0.0F : var0.getPitch();
   }

   public static void onTurn(double var0, double var2) {
      FreelookHook.Impl var4 = impl;
      if (var4 != null) {
         var4.onTurn(var0, var2);
      }
   }

   public static void absorbCamera(float var0, float var1) {
      FreelookHook.Impl var2 = impl;
      if (var2 != null) {
         var2.absorbCamera(var0, var1);
      }
   }

   public interface Impl {
      boolean isActive();

      float getYaw();

      float getPitch();

      void onTurn(double var1, double var3);

      void absorbCamera(float var1, float var2);
   }
}
