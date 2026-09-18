package dev.ambience.hooks;

import net.minecraft.block.BlockState;

public final class XRayHook {
   private static volatile XRayHook.Impl impl;

   private XRayHook() {
   }

   public static void bind(XRayHook.Impl var0) {
      impl = var0;
   }

   public static boolean active() {
      XRayHook.Impl var0 = impl;
      return var0 != null && var0.active();
   }

   public static boolean hides(BlockState var0) {
      XRayHook.Impl var1 = impl;
      return var1 != null && var1.hides(var0);
   }

   public static boolean hidesFluids() {
      XRayHook.Impl var0 = impl;
      return var0 != null && var0.hidesFluids();
   }

   public interface Impl {
      boolean active();

      boolean hides(BlockState var1);

      boolean hidesFluids();
   }
}
