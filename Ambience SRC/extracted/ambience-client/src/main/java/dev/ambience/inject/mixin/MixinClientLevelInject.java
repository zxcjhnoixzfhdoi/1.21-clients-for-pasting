package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.NoBlastHook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockParticleEffect;

import net.minecraft.util.math.Vec3d;

@Mixin(ClientWorld.class)
public class MixinClientLevelInject {
   @Inject(method = "addBlockParticleEffects", at = "HEAD", cancellable = true, captureArgs = true)
   public static void noBlastEffects(ClientWorld var0, Vec3d var1, float var2, int var3, Object var4) {
      if (NoBlastHook.enabled()) {
         Cancel.cancel();
      }
   }
}
