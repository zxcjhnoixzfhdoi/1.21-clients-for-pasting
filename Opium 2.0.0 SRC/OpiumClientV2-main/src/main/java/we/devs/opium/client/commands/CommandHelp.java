package we.devs.opium.client.commands;

import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.utilities.ChatUtils;

@RegisterCommand(name = "Help", description = "Gives information about the client", syntax = "help", aliases = {"help", "h"})
public class CommandHelp extends Command {

    @Override
    public void onCommand(String[] args) {
        ChatUtils.sendMessage("Welcome to 0piumh4ck.cc by heedi", "Help");
        ChatUtils.sendMessage("Opiumhack is on mc ver 1.21.1", "Help");
        ChatUtils.sendMessage("Bind the clickgui: .bind gui [bind]", "Help");
        ChatUtils.sendMessage("Enjoy your time here at Team Opium.", "Help");
    }
}
