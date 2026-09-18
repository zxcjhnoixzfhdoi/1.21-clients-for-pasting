package dev.ambience.hooks;

public final class JumpResetHook {
   private static volatile JumpResetHook.Impl impl;

   private JumpResetHook() {
   }

   public static void bind(JumpResetHook.Impl var0) {
      impl = var0;
   }

   public static void applyJump() {
      JumpResetHook.Impl var0 = impl;
      if (var0 != null) {
         var0.applyJump();
      }
   }

   public interface Impl {
      void applyJump();
   }
}
