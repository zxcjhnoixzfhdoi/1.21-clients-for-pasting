package hack.echo.client.mixin.network;

import hack.echo.client.Echo;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.event.impl.EventPacketSend;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinConnection {
    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void handlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventPacketReceive event = new EventPacketReceive(packet);
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void sendPacketEvent(Packet<?> packet, final CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventPacketSend event = new EventPacketSend(packet);
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void sendPacketEvent(Packet<?> packet, ChannelFutureListener listener, final CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventPacketSend event = new EventPacketSend(packet);
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void sendPacketEvent(Packet<?> packet, ChannelFutureListener listener, boolean shouldFlush, final CallbackInfo ci) {
        if (Echo.isDestroyed) return;

        EventPacketSend event = new EventPacketSend(packet);
        event.call();
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
