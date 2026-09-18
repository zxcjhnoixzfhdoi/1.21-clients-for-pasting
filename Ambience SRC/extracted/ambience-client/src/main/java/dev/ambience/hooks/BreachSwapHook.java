package dev.ambience.hooks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class BreachSwapHook {
   private static volatile BreachSwapHook.Impl impl;

   private BreachSwapHook() {
   }

   public static void bind(BreachSwapHook.Impl var0) {
      impl = var0;
   }

   public static void beforeAttack(PlayerEntity var0, Entity var1) {
      BreachSwapHook.Impl var2 = impl;
      if (var2 != null) {
         var2.beforeAttack(var0, var1);
      }
   }

   public static void afterAttack() {
      BreachSwapHook.Impl var0 = impl;
      if (var0 != null) {
         var0.afterAttack();
      }
   }

   public interface Impl {
      void beforeAttack(PlayerEntity var1, Entity var2);

      void afterAttack();
   }
}
