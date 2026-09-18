package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.hooks.AntiBlindHook;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

public class AntiBlind extends Module {
   private static AntiBlind INSTANCE;

   public AntiBlind() {
      super("AntiBlind", "Removes darkness and blindness overlays.", Module.Category.VISUALS);
      INSTANCE = this;
      AntiBlindHook.bind(new AntiBlindHook.Impl() {
         @Override
         public boolean active() {
            return AntiBlind.isActive();
         }

         @Override
         public boolean hides(RegistryEntry<StatusEffect> effect) {
            return AntiBlind.hides(effect);
         }
      });
   }

   public static AntiBlind get() {
      return INSTANCE;
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static boolean hides(RegistryEntry<StatusEffect> effect) {
      if (!isActive() || effect == null) {
         return false;
      }
      return effect == StatusEffects.DARKNESS || effect == StatusEffects.BLINDNESS;
   }
}
