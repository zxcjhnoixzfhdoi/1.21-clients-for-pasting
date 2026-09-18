package hack.echo.client.api;

import hack.echo.client.utils.Imports;
import net.minecraft.network.chat.Component;

public final class ChatCompat implements Imports {

    private ChatCompat() {
    }

    public static void addMessage(Component message) {
        if (mc.gui == null) {
            return;
        }

        //? if >26.1.2 {
        /*mc.gui.hud.getChat().addClientSystemMessage(message);
        *///?} else {
        //? if >=26.1 {
        /*mc.gui.getChat().addClientSystemMessage(message);
        *///?} else {
        mc.gui.getChat().addMessage(message);
        //?}
        //?}
    }
}
