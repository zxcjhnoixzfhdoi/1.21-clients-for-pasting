package hack.echo.client.api;

import net.minecraft.client.multiplayer.ClientLevel;

public final class ClientLevelCompat {

    private ClientLevelCompat() {
    }

    public static void setTimeFromServer(ClientLevel level, long time) {
        //? if >=26.1 {
        /*level.setTimeFromServer(time);
        *///?} else {
        level.setTimeFromServer(time, time, false);
        //?}
    }
}
