package dev.ambience.hooks;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public final class ViewModelHook {
   private static volatile ViewModelHook.Impl impl;

   private ViewModelHook() {
   }

   public static void bind(ViewModelHook.Impl var0) {
      impl = var0;
   }

   public static boolean enabled() {
      ViewModelHook.Impl var0 = impl;
      return var0 != null && var0.enabled();
   }

   public static boolean noSway() {
      ViewModelHook.Impl var0 = impl;
      return var0 != null && var0.noSway();
   }

   public static boolean noSwapAnimation() {
      ViewModelHook.Impl var0 = impl;
      return var0 != null && var0.noSwapAnimation();
   }

   public static boolean oldAnimation() {
      ViewModelHook.Impl var0 = impl;
      return var0 != null && var0.oldAnimation();
   }

   public static boolean applyArm(AbstractClientPlayerEntity var0, Hand var1, MatrixStack var2) {
      ViewModelHook.Impl var3 = impl;
      if (var3 != null && var3.enabled()) {
         var3.applyArm(var0, var1, var2);
         return true;
      } else {
         return false;
      }
   }

   public interface Impl {
      boolean enabled();

      boolean noSway();

      boolean noSwapAnimation();

      boolean oldAnimation();

      void applyArm(AbstractClientPlayerEntity var1, Hand var2, MatrixStack var3);
   }
}
