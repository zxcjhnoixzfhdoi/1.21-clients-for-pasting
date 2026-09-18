package dev.ambience.hooks;

public final class ProjectileAimHook {
   private static volatile ProjectileAimHook.Impl impl;

   private ProjectileAimHook() {
   }

   public static void bind(ProjectileAimHook.Impl var0) {
      impl = var0;
   }

   public static void onMouseTurn(double var0) {
      ProjectileAimHook.Impl var2 = impl;
      if (var2 != null) {
         var2.onMouseTurn(var0);
      }
   }

   public interface Impl {
      void onMouseTurn(double var1);
   }
}
