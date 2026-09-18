package dev.ambience.hooks;

import net.minecraft.util.math.Vec3d;

public final class FreecamHook {
   private static volatile FreecamHook.Impl impl;

   private FreecamHook() {
   }

   public static void bind(FreecamHook.Impl var0) {
      impl = var0;
   }

   public static boolean isActive() {
      FreecamHook.Impl var0 = impl;
      return var0 != null && var0.isActive();
   }

   public static boolean blocksInteraction() {
      FreecamHook.Impl var0 = impl;
      return var0 != null && var0.blocksInteraction();
   }

   public static float getYaw() {
      FreecamHook.Impl var0 = impl;
      return var0 == null ? 0.0F : var0.getYaw();
   }

   public static float getPitch() {
      FreecamHook.Impl var0 = impl;
      return var0 == null ? 0.0F : var0.getPitch();
   }

   public static Vec3d getCameraPos(float var0) {
      FreecamHook.Impl var1 = impl;
      return var1 == null ? Vec3d.ZERO : var1.getCameraPos(var0);
   }

   public static void onTurn(double var0, double var2) {
      FreecamHook.Impl var4 = impl;
      if (var4 != null) {
         var4.onTurn(var0, var2);
      }
   }

   public static void freezeInput() {
      FreecamHook.Impl var0 = impl;
      if (var0 != null) {
         var0.freezeInput();
      }
   }

   public interface Impl {
      boolean isActive();

      boolean blocksInteraction();

      float getYaw();

      float getPitch();

      Vec3d getCameraPos(float var1);

      void onTurn(double var1, double var3);

      void freezeInput();
   }
}
