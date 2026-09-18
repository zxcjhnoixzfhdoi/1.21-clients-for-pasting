package onl.luka.grizzly.mixin.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import onl.luka.grizzly.module.modules.combat.AutoBlock;
import onl.luka.grizzly.module.modules.combat.Backtrack;
import onl.luka.grizzly.module.modules.combat.Misplace;
import onl.luka.grizzly.module.modules.combat.Velocity;
import onl.luka.grizzly.module.modules.player.ClientBrand;
import onl.luka.grizzly.module.modules.combat.KnockbackDelay;
import onl.luka.grizzly.module.modules.exploits.AntiAura;
import onl.luka.grizzly.module.modules.exploits.AntiHunger;
import onl.luka.grizzly.module.modules.exploits.Disabler;
import onl.luka.grizzly.module.modules.exploits.NoSwing;
import onl.luka.grizzly.module.modules.exploits.PortalGodmode;
import onl.luka.grizzly.module.modules.movement.Flight;
import onl.luka.grizzly.module.modules.movement.Stasis;
import onl.luka.grizzly.module.modules.movement.InvMove;
import onl.luka.grizzly.module.modules.movement.Phase;
import onl.luka.grizzly.module.modules.movement.Step;
import onl.luka.grizzly.util.LagManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {

    @Shadow private volatile PacketListener packetListener;

    // Flight rewrites every movement packet, so without this the resend below
    // would be rewritten and resent again forever.
    @Unique private boolean medved$resendingMovement = false;

    @SuppressWarnings("unchecked")
    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void medved$onChannelRead(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        //System.out.println(packet.type().toString() + " " + packet.toString());
        PacketListener listener = packetListener;
        if (!(listener instanceof ClientPacketListener)) return;

        if (LagManager.INSTANCE.shouldBufferIncoming()) {
            Packet<PacketListener> p = (Packet<PacketListener>) packet;
            ci.cancel();
            if (Minecraft.getInstance().level != null) {
                Entity entity = null;
                if (packet instanceof ClientboundMoveEntityPacket move) {
                    entity = move.getEntity(Minecraft.getInstance().level);
                }

                if (entity != null && Backtrack.INSTANCE.isEnabled()
                        && Backtrack.INSTANCE.getMode().getValue() == Backtrack.Mode.LAG
                        && (!Backtrack.INSTANCE.getOnlyPlayers().getValue() || entity instanceof Player)) {

                    Vec3 currentPos = Backtrack.INSTANCE.getRealPositions().getOrDefault(entity.getId(), entity.position());

                    if (packet instanceof ClientboundMoveEntityPacket move) {
                        Backtrack.INSTANCE.updateRealPosition(entity.getId(), new Vec3(
                                currentPos.x + (move.getXa() / 4096.0),
                                currentPos.y + (move.getYa() / 4096.0),
                                currentPos.z + (move.getZa() / 4096.0)
                        ));
                    }
                }
            }
            LagManager.INSTANCE.bufferIncoming(() -> p.handle(listener));
            return;
        }
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void medved$onSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        Connection conn = (Connection)(Object) this;
        if (packet instanceof ServerboundMovePlayerPacket movementPacket && !medved$resendingMovement) {
            ServerboundMovePlayerPacket adjusted =
                    Disabler.adjustOutgoingMovementPacket(
                            AntiAura.adjustOutgoingMovementPacket(
                                    AntiHunger.adjustOutgoingMovementPacket(
                                            Flight.INSTANCE.adjustOutgoingMovementPacket(
                                                    Step.adjustOutgoingMovementPacket(
                                                            Phase.adjustOutgoingMovementPacket(movementPacket))))));
            if (adjusted != movementPacket) {
                ci.cancel();
                medved$resendingMovement = true;
                try {
                    conn.send(adjusted, listener, flush);
                } finally {
                    medved$resendingMovement = false;
                }
                return;
            }
        }

        if (NoSwing.shouldCancelPacket(packet)
                || PortalGodmode.shouldCancelPacket(packet)
                || AntiAura.shouldCancelPacket(packet)) {
            ci.cancel();
            return;
        }
        if (Stasis.shouldCancelPacket(packet)) {
            ci.cancel();
            return;
        }
        if (InvMove.handleOutgoingPacket(packet)) {
            ci.cancel();
            return;
        }
        if (packet instanceof ServerboundPongPacket
                && Velocity.INSTANCE.isEnabled()
                && Velocity.INSTANCE.getMode().getValue() == Velocity.Mode.FREEZE) {
            Velocity.INSTANCE.onFreezePong();
        }

        // Brand spoofer: intercept the outgoing brand custom payload and replace it.
        // The re-entry guard prevents infinite recursion when we call conn.send() with the replacement.
        if (AutoBlock.isBlocking && packet instanceof ServerboundPlayerActionPacket &&
                ((ServerboundPlayerActionPacket) packet).getAction() == net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            ci.cancel();
            return;
        }

        if (!ClientBrand.INSTANCE.isResending
                && ClientBrand.INSTANCE.isEnabled()
                && packet instanceof ServerboundCustomPayloadPacket cp
                && cp.payload() instanceof BrandPayload) {
            ci.cancel();
            ClientBrand.INSTANCE.isResending = true;
            conn.send(
                    new ServerboundCustomPayloadPacket(new BrandPayload(ClientBrand.INSTANCE.getCurrentBrand())),
                    listener,
                    flush
            );
            ClientBrand.INSTANCE.isResending = false;
            return;
        }

        try {
            if (LagManager.INSTANCE.shouldBufferOutgoing()) {
                ci.cancel();
                final long queuedAt = System.currentTimeMillis();
                LagManager.INSTANCE.bufferOutgoing(() -> {
                    if (packet instanceof ServerboundPongPacket) {
                        long age = System.currentTimeMillis() - queuedAt;

                        if (age > 5000L) {
                            return; // drop stale pong
                        }
                    }

                    conn.send(packet, listener, flush);
                });
            } else {
                Misplace.onOutgoingSend(packet);
            }
        } catch (Exception e) {
            // Failsafe escape hatch
        }
    }
}
