package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.command.CommandEngine;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void medved$onChatInput(String input, boolean addToHistory, CallbackInfo ci) {
        Character clientPrefix = CommandEngine.INSTANCE.clientPrefixChar();
        if (clientPrefix == null) return;
        String normalized = ((ChatScreen) (Object) this).normalizeChatMessage(input);
        if (normalized.startsWith(clientPrefix.toString())) {
            CommandEngine.INSTANCE.executeClientCommand(normalized);
            ci.cancel();
        }
    }
}
