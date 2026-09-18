package dev.ambience.inject.mixin;

import com.mixininject.api.Cancel;
import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import dev.ambience.hooks.CrystalOptimizerHook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPacketListenerInject {
   @Inject(method = "onExplosion", at = "HEAD", cancellable = true, captureArgs = true)
   public static void skipPredictedCrystalBoom(ClientPlayNetworkHandler var0, ExplosionS2CPacket var1) {
      if (CrystalOptimizerHook.consumePredictedExplosion(var1.center())) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         if (var2.player != null) {
            var1.playerKnockback().ifPresent(var2.player::addVelocityInternal);
         }

         Cancel.cancel();
      }
   }
}
