package dev.ambience.hooks;

public final class AutoTotemHook {
   private static volatile AutoTotemHook.Impl impl;

   private AutoTotemHook() {
   }

   public static void bind(AutoTotemHook.Impl var0) {
      impl = var0;
   }

   public static boolean blocksInteraction() {
      AutoTotemHook.Impl var0 = impl;
      return var0 != null && var0.blocksInteraction();
   }

   public static boolean blocksInventoryInput() {
      AutoTotemHook.Impl var0 = impl;
      return var0 != null && var0.blocksInventoryInput();
   }

   public static void freezeInput() {
      AutoTotemHook.Impl var0 = impl;
      if (var0 != null) {
         var0.freezeInput();
      }
   }

   public interface Impl {
      boolean blocksInteraction();

      boolean blocksInventoryInput();

      void freezeInput();
   }
}
