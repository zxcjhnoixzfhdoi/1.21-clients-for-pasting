package dev.ambience.mixin.entity;

import dev.ambience.hooks.FreecamHook;
import dev.ambience.hooks.FreelookHook;
import dev.ambience.util.SilentAim;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinSilentRotation {
   @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
   private void ambience$trackClientTurn(double var1, double var3, CallbackInfo var5) {
      if ((Object)this instanceof ClientPlayerEntity) {
         if (FreecamHook.isActive()) {
            FreecamHook.onTurn(var1, var3);
            var5.cancel();
         } else if (SilentAim.a()) {
            SilentAim.a(var1, var3);
            var5.cancel();
         } else {
            if (FreelookHook.isActive()) {
               FreelookHook.onTurn(var1, var3);
               var5.cancel();
            }
         }
      }
   }
}
