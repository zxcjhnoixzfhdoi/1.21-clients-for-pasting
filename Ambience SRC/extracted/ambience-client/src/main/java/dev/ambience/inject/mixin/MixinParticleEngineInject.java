package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
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

@Mixin(ParticleManager.class)
public class MixinParticleEngineInject {
   @Inject(method = "addParticle", at = "HEAD", cancellable = true, captureArgs = true)
   public static void noBlastCreate(ParticleManager var0, ParticleEffect var1, double var2, double var4, double var6, double var8, double var10, double var12) {
      if (NoBlastHook.shouldCancel(var1) || NoSmokeHook.shouldCancel(var1)) {
         Cancel.cancel(null);
      }
   }

   @Inject(method = "addParticle", at = "HEAD", cancellable = true, captureArgs = true)
   public static void noBlastAdd(ParticleManager var0, Particle var1) {
      if (!NoBlastHook.enabled() || !(var1 instanceof ExplosionLargeParticle) && !(var1 instanceof ExplosionEmitterParticle)) {
         if (NoSmokeHook.enabled()
            && (
               var1 instanceof FireSmokeParticle
                  || var1 instanceof LargeFireSmokeParticle
                  || var1 instanceof WhiteSmokeParticle
                  || var1 instanceof CampfireSmokeParticle
            )) {
            Cancel.cancel();
         }
      } else {
         Cancel.cancel();
      }
   }
}
