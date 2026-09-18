package dev.ambience.mixin.client;

import dev.ambience.hooks.NoBlastHook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockParticleEffect;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class MixinClientLevel {
   @Inject(method = "addBlockParticleEffects", at = @At("HEAD"), cancellable = true)
   private void ambience$noBlastEffects(Vec3d var1, float var2, int var3, Object var4, CallbackInfo var5) {
      if (NoBlastHook.enabled()) {
         var5.cancel();
      }
   }
}
