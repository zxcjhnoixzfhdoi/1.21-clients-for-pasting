package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.utils.IMinecraft;
import com.instrumentalist.krs.hacks.ModuleManager;
import com.instrumentalist.krs.hacks.features.player.ChatCommands;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.breadloaf.imguimc.customwindow.ModuleRenderable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin implements IMinecraft {

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void chatCommandHook(String message, boolean addToHistory, CallbackInfo ci) {
        if (ModuleManager.getModuleState(ChatCommands.class) && message.startsWith(ChatCommands.prefix.get())) {
            ci.cancel();
            ModuleRenderable.executeCommand(message.substring(ChatCommands.prefix.get().length()), true);
            mc.gui.hud.getChat().addRecentChat(message);
        }
    }
}
