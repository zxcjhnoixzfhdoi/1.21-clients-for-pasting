package dev.ambience.hooks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class ShieldStunHook {
   private static volatile ShieldStunHook.Impl impl;

   private ShieldStunHook() {
   }

   public static void bind(ShieldStunHook.Impl var0) {
      impl = var0;
   }

   public static void beforeAttack(PlayerEntity var0, Entity var1) {
      ShieldStunHook.Impl var2 = impl;
      if (var2 != null) {
         var2.beforeAttack(var0, var1);
      }
   }

   public static void afterAttack() {
      ShieldStunHook.Impl var0 = impl;
      if (var0 != null) {
         var0.afterAttack();
      }
   }

   public interface Impl {
      void beforeAttack(PlayerEntity var1, Entity var2);

      void afterAttack();
   }
}
