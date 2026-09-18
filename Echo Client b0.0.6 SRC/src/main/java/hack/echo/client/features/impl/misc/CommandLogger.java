package hack.echo.client.features.impl.misc;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventPacketSend;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.api.ChatCompat;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.chat.Component;

public class CommandLogger extends Feature {

    public CommandLogger() {
        super(new FeatureInfo(
            Concat.of("Command Logger"),
            Concat.of("Logs commands"),
            Category.INTERNALS
        ));
    }

    @EventSubscribe
    private void onPacketSend(EventPacketSend event) {
        if (mc.player == null) {
            return;
        }

        long currentTimeMs = System.currentTimeMillis();

        if (event.getPacket() instanceof ServerboundChatCommandPacket packet) {
            String command = packet.command();
            logCommand(Concat.of("/", command), currentTimeMs);
        }
        
        if (event.getPacket() instanceof ServerboundChatCommandSignedPacket packet) {
            String command = packet.command();
            logCommand(Concat.of("/", command), currentTimeMs);
        }
        
        if (event.getPacket() instanceof ServerboundChatPacket packet) {
            String message = packet.message();
            logMessage(message, currentTimeMs);
        }
    }

    public static void logCommand(CharSequence command, long timestampMs) {
        Concat timestamp = Concat.ofChars(Long.toString(timestampMs).toCharArray());
        System.out.println(Concat.of("[", timestamp, "ms] Command: ", command));
        
        if (mc.gui != null) {
            ChatCompat.addMessage(
                Component.literal(Concat.of("§7[§b", timestamp, "ms§7] §aCommand: §f", command).toString())
            );
        }
    }

    private void logMessage(String message, long timestampMs) {
        Concat timestamp = Concat.ofChars(Long.toString(timestampMs).toCharArray());
        System.out.println(Concat.of("[", timestamp, "ms] Message: ", message));
        
        if (mc.gui != null) {
            ChatCompat.addMessage(
                Component.literal(Concat.of("§7[§b", timestamp, "ms§7] §eMessage: §f", message).toString())
            );
        }
    }
}
