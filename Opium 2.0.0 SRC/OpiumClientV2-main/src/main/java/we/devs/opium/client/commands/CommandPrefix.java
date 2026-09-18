package we.devs.opium.client.commands;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.modules.client.ModuleCommands;

@RegisterCommand(name="Prefix", description="Let's you change your command prefix.", syntax="prefix <input>", aliases={"commandprefix", "cmdprefix", "commandp", "cmdp"})
public class CommandPrefix extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 1) {
            if (args[0].length() > 2) {
                ChatUtils.sendMessage("The prefix must not be longer than 2 characters.", "Prefix");
            } else {
                Opium.COMMAND_MANAGER.setPrefix(args[0]);
                ChatUtils.sendMessage("Prefix set to \"" + ModuleCommands.getSecondColor() + Opium.COMMAND_MANAGER.getPrefix() + ModuleCommands.getFirstColor() + "\"!", "Prefix");
            }
        } else {
            this.sendSyntax();
        }
    }
}
