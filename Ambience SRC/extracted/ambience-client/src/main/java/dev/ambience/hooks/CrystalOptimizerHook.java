package dev.ambience.hooks;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class CrystalOptimizerHook {
   private static volatile CrystalOptimizerHook.Impl impl;

   private CrystalOptimizerHook() {
   }

   public static void bind(CrystalOptimizerHook.Impl var0) {
      impl = var0;
   }

   public static void afterAttack(Entity var0) {
      CrystalOptimizerHook.Impl var1 = impl;
      if (var1 != null) {
         var1.afterAttack(var0);
      }
   }

   public static boolean consumePredictedExplosion(Vec3d var0) {
      CrystalOptimizerHook.Impl var1 = impl;
      return var1 != null && var1.consumePredictedExplosion(var0);
   }

   public interface Impl {
      void afterAttack(Entity var1);

      boolean consumePredictedExplosion(Vec3d var1);
   }
}
