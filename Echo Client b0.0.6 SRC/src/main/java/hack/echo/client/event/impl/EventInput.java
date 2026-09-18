package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import net.minecraft.client.player.ClientInput;

import static hack.echo.client.utils.Imports.mc;

public class EventInput extends Event {

    public ClientInput input;
    public boolean check;

    public EventInput(ClientInput input) {
        this.input = input;
        this.check = (mc.player != null && input == mc.player.input);
    }
}