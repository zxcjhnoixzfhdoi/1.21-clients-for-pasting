package we.devs.opium.client.commands;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.utilities.ChatUtils;

import java.io.IOException;

@RegisterCommand(name="config", description="Let's you save or load your configuration without restarting the game.", syntax="config <save|load|delete> <name>", aliases={"configuration", "cfg"})
public class CommandConfig extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("load")) {
                if (Opium.CONFIG_MANAGER.getAvailableConfigs().contains(args[1])) {
                    try {
                        Opium.CONFIG_MANAGER.loadConfig(args[1]);
                        ChatUtils.sendMessage("Successfully loaded configuration [" + args[1] + "]!", "Config");
                    } catch (IOException e) {
                        ChatUtils.sendMessage("Could not load configuration [" + args[1] + "] :(", "Config");
                    }
                } else {
                    ChatUtils.sendMessage("Could not find configuration file [" + args[1] + "] :(", "Config");
                }
            } else if (args[0].equalsIgnoreCase("save")) {
                try {
                    Opium.CONFIG_MANAGER.saveConfig(args[1]);
                    ChatUtils.sendMessage("Successfully saved configuration " + args[1] + "!", "Config");
                } catch (IOException e) {
                    ChatUtils.sendMessage("Could not save configuration [" + args[1] + "] :(", "Config");
                }
            } else if (args[0].equalsIgnoreCase("delete")) {
                if (Opium.CONFIG_MANAGER.getAvailableConfigs().contains(args[1])) {
                    Opium.CONFIG_MANAGER.delete(args[1]);
                    ChatUtils.sendMessage("Successfully Deleted " + args[1] + "!", "Config");
                } else ChatUtils.sendMessage("Could not find configuration file [" + args[1] + "] :(", "Config");
            } else {
                this.sendSyntax();
            }
        } else {
            this.sendSyntax();
        }
    }
}