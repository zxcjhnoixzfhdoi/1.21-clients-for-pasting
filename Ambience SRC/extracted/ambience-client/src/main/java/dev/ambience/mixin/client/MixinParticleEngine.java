package dev.ambience.mixin.client;

import dev.ambience.hooks.NoBlastHook;
import dev.ambience.hooks.NoSmokeHook;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.ExplosionEmitterParticle;
import net.minecraft.client.particle.ExplosionLargeParticle;
import net.minecraft.client.particle.FireSmokeParticle;
import net.minecraft.client.particle.LargeFireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.WhiteSmokeParticle;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class MixinParticleEngine {
   @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
   private void ambience$noBlastCreate(
      ParticleEffect var1, double var2, double var4, double var6, double var8, double var10, double var12, CallbackInfoReturnable<Particle> var14
   ) {
      if (NoBlastHook.shouldCancel(var1)) {
         var14.setReturnValue(null);
      } else {
         if (NoSmokeHook.shouldCancel(var1)) {
            var14.setReturnValue(null);
         }
      }
   }

   @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
   private void ambience$noBlastAdd(Particle var1, CallbackInfo var2) {
      if (!NoBlastHook.enabled() || !(var1 instanceof ExplosionLargeParticle) && !(var1 instanceof ExplosionEmitterParticle)) {
         if (NoSmokeHook.enabled()
            && (
               var1 instanceof FireSmokeParticle
                  || var1 instanceof LargeFireSmokeParticle
                  || var1 instanceof WhiteSmokeParticle
                  || var1 instanceof CampfireSmokeParticle
            )) {
            var2.cancel();
         }
      } else {
         var2.cancel();
      }
   }
}
