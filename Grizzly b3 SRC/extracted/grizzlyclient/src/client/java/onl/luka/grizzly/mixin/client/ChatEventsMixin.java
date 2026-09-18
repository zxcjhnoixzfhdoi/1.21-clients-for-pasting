package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.command.CommandEngine;
import onl.luka.grizzly.module.modules.exploits.ChatBypass;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ChatEventsMixin {

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true)
    private String medved$mapOutgoingChat(String message) {
        return ChatBypass.mapChat(message);
    }

    @ModifyVariable(method = "sendCommand", at = @At("HEAD"), argsOnly = true)
    private String medved$mapOutgoingCommand(String command) {
        return ChatBypass.mapCommand(command);
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void medved$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (packet.overlay()) return;
        CommandEngine.INSTANCE.handleIncomingChat(packet.content().getString());
    }

    @Inject(method = "handlePlayerChat", at = @At("HEAD"))
    private void medved$onPlayerChat(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
        CommandEngine.INSTANCE.handleIncomingChat(packet.body().content());
    }
}
