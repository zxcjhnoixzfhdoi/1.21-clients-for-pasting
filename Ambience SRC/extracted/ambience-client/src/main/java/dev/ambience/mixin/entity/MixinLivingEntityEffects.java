package dev.ambience.mixin.entity;

import dev.ambience.hooks.AntiBlindHook;
import dev.ambience.util.traits.Util;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntityEffects {
   @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
   private void ambience$antiBlindHas(RegistryEntry<StatusEffect> var1, CallbackInfoReturnable<Boolean> var2) {
      if ((Object)this == Util.mc.player) {
         if (AntiBlindHook.hides(var1)) {
            var2.setReturnValue(false);
         }
      }
   }

   @Inject(method = "getStatusEffect", at = @At("HEAD"), cancellable = true)
   private void ambience$antiBlindGet(RegistryEntry<StatusEffect> var1, CallbackInfoReturnable<StatusEffectInstance> var2) {
      if ((Object)this == Util.mc.player) {
         if (AntiBlindHook.hides(var1)) {
            var2.setReturnValue(null);
         }
      }
   }
}
