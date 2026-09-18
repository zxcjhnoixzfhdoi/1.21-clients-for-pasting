package dev.ambience.hooks;

import net.minecraft.client.network.ClientPlayerEntity;

public final class AutoSprintHook {
   private static volatile AutoSprintHook.Impl impl;

   private AutoSprintHook() {
   }

   public static void bind(AutoSprintHook.Impl var0) {
      impl = var0;
   }

   public static void apply(ClientPlayerEntity var0) {
      AutoSprintHook.Impl var1 = impl;
      if (var1 != null) {
         var1.apply(var0);
      }
   }

   public interface Impl {
      void apply(ClientPlayerEntity var1);
   }
}
