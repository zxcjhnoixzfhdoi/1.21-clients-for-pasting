//? if debug {
/*package hack.echo.client.command.impl;

import hack.echo.client.command.Command;
import hack.echo.client.handlers.impl.BroadcastHandler;
import hack.echo.client.utils.ChatUtils;

public class Broadcast extends Command {

    public Broadcast() {
        super("broadcast", "Dev test command");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtils.chat("usage: .broadcast <message>");
            return;
        }

        BroadcastHandler.addMessage(String.join(" ", args));
    }
}
*///?}