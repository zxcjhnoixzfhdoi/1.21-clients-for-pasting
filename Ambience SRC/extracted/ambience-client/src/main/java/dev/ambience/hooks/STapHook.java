package dev.ambience.hooks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class STapHook {
   private static volatile STapHook.Impl impl;

   private STapHook() {
   }

   public static void bind(STapHook.Impl var0) {
      impl = var0;
   }

   public static void beforeAttack(PlayerEntity var0, Entity var1) {
      STapHook.Impl var2 = impl;
      if (var2 != null) {
         var2.beforeAttack(var0, var1);
      }
   }

   public static void afterAttack() {
      STapHook.Impl var0 = impl;
      if (var0 != null) {
         var0.afterAttack();
      }
   }

   public static void freezeInput() {
      STapHook.Impl var0 = impl;
      if (var0 != null) {
         var0.freezeInput();
      }
   }

   public interface Impl {
      void beforeAttack(PlayerEntity var1, Entity var2);

      void afterAttack();

      void freezeInput();
   }
}
