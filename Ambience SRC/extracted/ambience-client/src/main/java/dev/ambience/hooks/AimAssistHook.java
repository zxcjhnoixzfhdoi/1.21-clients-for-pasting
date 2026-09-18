package dev.ambience.hooks;

public final class AimAssistHook {
   private static volatile AimAssistHook.Impl impl;

   private AimAssistHook() {
   }

   public static void bind(AimAssistHook.Impl var0) {
      impl = var0;
   }

   public static void onMouseTurn(double var0) {
      AimAssistHook.Impl var2 = impl;
      if (var2 != null) {
         var2.onMouseTurn(var0);
      }
   }

   public interface Impl {
      void onMouseTurn(double var1);
   }
}
