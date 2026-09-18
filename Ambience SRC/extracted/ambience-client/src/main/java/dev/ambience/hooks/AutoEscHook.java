package dev.ambience.hooks;

public final class AutoEscHook {
   private static volatile AutoEscHook.Impl impl;

   private AutoEscHook() {
   }

   public static void bind(AutoEscHook.Impl var0) {
      impl = var0;
   }

   public static boolean blocksInteraction() {
      AutoEscHook.Impl var0 = impl;
      return var0 != null && var0.blocksInteraction();
   }

   public interface Impl {
      boolean blocksInteraction();
   }
}
