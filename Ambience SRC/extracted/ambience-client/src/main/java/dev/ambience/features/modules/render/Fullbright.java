package dev.ambience.features.modules.render;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.FullbrightHook;
import dev.ambience.inject.Access;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

public class Fullbright extends Module {
   private static Fullbright INSTANCE;
   private final Setting<Mode> renderMode;

   public Fullbright() {
      super("Fullbright", "Lights up your world.", Module.Category.VISUALS);
      this.renderMode = this.mode("Mode", Mode.Gamma).setPage("General");
      INSTANCE = this;
      FullbrightHook.bind(this::gammaActive);
   }

   public static Fullbright get() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      if (this.renderMode.getValue() == Mode.Potion) {
         removeNightVision();
      }
   }

   @Override
   public void onTick() {
      if (!nullCheck() || this.renderMode.getValue() != Mode.Potion) {
         return;
      }
      RegistryEntry<StatusEffect> nightVision =
         Registries.STATUS_EFFECT.getEntry((StatusEffect) StatusEffects.NIGHT_VISION.value());
      if (mc.player.hasStatusEffect(nightVision)) {
         StatusEffectInstance effect = mc.player.getStatusEffect(nightVision);
         if (effect != null && effect.getDuration() < 220) {
            Access.setEffectDuration(effect, Short.MAX_VALUE);
         }
      } else {
         mc.player.addStatusEffect(new StatusEffectInstance(nightVision, Short.MAX_VALUE, 0, false, false, false));
      }
   }

   public boolean gammaActive() {
      return this.isEnabled() && this.renderMode.getValue() == Mode.Gamma;
   }

   private void removeNightVision() {
      if (!nullCheck()) return;
      RegistryEntry<StatusEffect> nightVision =
         Registries.STATUS_EFFECT.getEntry((StatusEffect) StatusEffects.NIGHT_VISION.value());
      if (mc.player.hasStatusEffect(nightVision)) {
         mc.player.removeStatusEffect(nightVision);
      }
   }

   public enum Mode {
      Gamma,
      Potion
   }
}
