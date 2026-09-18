package dev.ambience.hooks;

import net.minecraft.client.network.ClientPlayerEntity;

public final class OmniSprintHook {
   private static volatile OmniSprintHook.Impl impl;

   private OmniSprintHook() {
   }

   public static void bind(OmniSprintHook.Impl var0) {
      impl = var0;
   }

   public static void apply(ClientPlayerEntity var0) {
      OmniSprintHook.Impl var1 = impl;
      if (var1 != null) {
         var1.apply(var0);
      }
   }

   public interface Impl {
      void apply(ClientPlayerEntity var1);
   }
}
