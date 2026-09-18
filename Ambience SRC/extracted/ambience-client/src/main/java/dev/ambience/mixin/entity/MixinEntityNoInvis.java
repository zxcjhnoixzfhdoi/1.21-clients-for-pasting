package dev.ambience.mixin.entity;

import dev.ambience.hooks.NoInvisHook;
import dev.ambience.util.traits.Util;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntityNoInvis {
   @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
   private void ambience$showInvisibleEntities(CallbackInfoReturnable<Boolean> var1) {
      if (NoInvisHook.enabled() && Util.mc.player != null) {
         if ((Object)this != Util.mc.player) {
            var1.setReturnValue(false);
         }
      }
   }
}
