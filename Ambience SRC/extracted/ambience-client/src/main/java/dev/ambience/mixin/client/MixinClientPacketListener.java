package dev.ambience.mixin.client;

import dev.ambience.hooks.CrystalOptimizerHook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPacketListener {
   @Inject(method = "onExplosion", at = @At("HEAD"), cancellable = true)
   private void ambience$skipPredictedCrystalBoom(ExplosionS2CPacket var1, CallbackInfo var2) {
      if (CrystalOptimizerHook.consumePredictedExplosion(var1.center())) {
         MinecraftClient var3 = MinecraftClient.getInstance();
         if (var3.player != null) {
            var1.playerKnockback().ifPresent(var3.player::addVelocityInternal);
         }

         var2.cancel();
      }
   }
}
