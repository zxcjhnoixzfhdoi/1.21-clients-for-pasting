package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.AntiBlindHook;
import dev.ambience.util.traits.Util;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

@Mixin(LivingEntity.class)
public class MixinLivingEntityEffectsInject {
   @Inject(method = "hasStatusEffect", at = "HEAD", cancellable = true, captureArgs = true)
   public static void hasEffect(LivingEntity var0, RegistryEntry<StatusEffect> var1) {
      if (var0 == Util.mc.player) {
         if (AntiBlindHook.hides(var1)) {
            Cancel.cancel(Boolean.FALSE);
         }
      }
   }

   @Inject(method = "getStatusEffect", at = "HEAD", cancellable = true, captureArgs = true)
   public static void getEffect(LivingEntity var0, RegistryEntry<StatusEffect> var1) {
      if (var0 == Util.mc.player) {
         if (AntiBlindHook.hides(var1)) {
            Cancel.cancel(null);
         }
      }
   }
}
