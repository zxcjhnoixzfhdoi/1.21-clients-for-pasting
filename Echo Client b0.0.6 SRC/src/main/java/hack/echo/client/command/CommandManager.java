package hack.echo.client.command;

//? if debug
//import hack.echo.client.command.impl.Broadcast;
import hack.echo.client.command.impl.Config;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();

    public CommandManager() {
        //? if debug
        //commands.add(new Broadcast());
        commands.add(new Config());
    }

    public Command getCommand(String name) {
        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                return command;
            }
        }

        return null;
    }
}
