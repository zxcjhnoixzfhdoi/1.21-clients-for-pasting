package dev.ambience.hooks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class WTapHook {
   private static volatile WTapHook.Impl impl;

   private WTapHook() {
   }

   public static void bind(WTapHook.Impl var0) {
      impl = var0;
   }

   public static void beforeAttack(PlayerEntity var0, Entity var1) {
      WTapHook.Impl var2 = impl;
      if (var2 != null) {
         var2.beforeAttack(var0, var1);
      }
   }

   public static void afterAttack() {
      WTapHook.Impl var0 = impl;
      if (var0 != null) {
         var0.afterAttack();
      }
   }

   public static void freezeInput() {
      WTapHook.Impl var0 = impl;
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
