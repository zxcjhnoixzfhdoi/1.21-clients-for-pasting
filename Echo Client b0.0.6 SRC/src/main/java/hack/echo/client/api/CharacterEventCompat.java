package hack.echo.client.api;

import net.minecraft.client.input.CharacterEvent;

public final class CharacterEventCompat {

    private CharacterEventCompat() {
    }

    public static int modifiers(CharacterEvent event) {
        //? if >=26.1 {
        /*return 0;
        *///?} else {
        return event.modifiers();
        //?}
    }
}
