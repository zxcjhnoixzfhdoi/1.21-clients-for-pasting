package dev.ambience.hooks;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

public final class AntiBlindHook {
   private static volatile AntiBlindHook.Impl impl;

   private AntiBlindHook() {
   }

   public static void bind(AntiBlindHook.Impl var0) {
      impl = var0;
   }

   public static boolean active() {
      AntiBlindHook.Impl var0 = impl;
      return var0 != null && var0.active();
   }

   public static boolean hides(RegistryEntry<StatusEffect> var0) {
      AntiBlindHook.Impl var1 = impl;
      return var1 != null && var1.hides(var0);
   }

   public interface Impl {
      boolean active();

      boolean hides(RegistryEntry<StatusEffect> var1);
   }
}
