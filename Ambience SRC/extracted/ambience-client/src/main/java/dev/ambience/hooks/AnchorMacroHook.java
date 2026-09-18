package dev.ambience.hooks;

public final class AnchorMacroHook {
   private static volatile AnchorMacroHook.Impl impl;

   private AnchorMacroHook() {
   }

   public static void bind(AnchorMacroHook.Impl var0) {
      impl = var0;
   }

   public static boolean blocksInteraction() {
      AnchorMacroHook.Impl var0 = impl;
      return var0 != null && var0.blocksInteraction();
   }

   public interface Impl {
      boolean blocksInteraction();
   }
}
