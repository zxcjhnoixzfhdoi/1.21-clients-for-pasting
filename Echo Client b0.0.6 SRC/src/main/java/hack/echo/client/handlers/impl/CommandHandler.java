package hack.echo.client.handlers.impl;

import hack.echo.client.Echo;
import hack.echo.client.command.Command;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventPacketSend;
import hack.echo.client.handlers.Handler;
import hack.echo.client.utils.ChatUtils;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

import java.util.Arrays;
import java.util.List;

public class CommandHandler extends Handler {

    @EventSubscribe
    private void onPacketSend(EventPacketSend event) {
        if (!(event.getPacket() instanceof ServerboundChatPacket packet)) return;

        String message = packet.message();
        if (!message.startsWith(".")) {
            return;
        }

        event.cancel();
        if (mc.gui == null) return;

        String[] parts = message.trim().split("\\s+");
        Command command = Echo.commandManager.getCommand(parts[0].replace(".", ""));
        if (command == null) {
            ChatUtils.chat("Unknown command. Type .help for a list of commands.");
            return;
        }

        command.execute(Arrays.copyOfRange(parts, 1, parts.length));
    }
}
