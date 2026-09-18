package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.utility.anticheat.CheatMarker;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true)
    private Component medved$markKnownCheaters(Component message) {
        if (message == null) return null;
        return CheatMarker.INSTANCE.decorateChat(message);
    }
}
