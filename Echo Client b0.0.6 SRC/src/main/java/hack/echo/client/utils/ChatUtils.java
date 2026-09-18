package hack.echo.client.utils;

import hack.echo.client.api.ChatCompat;
import net.minecraft.network.chat.Component;

public class ChatUtils implements Imports {
    public static void chat(String text) {
        ChatCompat.addMessage(Component.literal(text));
    }

    public static void chat(CharSequence text) {
        ChatCompat.addMessage(Component.literal(text.toString()));
    }
}
