package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.command.TpsTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds the server game time from time-sync packets into the TPS tracker.
 */
@Mixin(ClientPacketListener.class)
public class TpsTrackerMixin {

    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void medved$onSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        TpsTracker.INSTANCE.onServerTime(packet.gameTime());
    }
}
