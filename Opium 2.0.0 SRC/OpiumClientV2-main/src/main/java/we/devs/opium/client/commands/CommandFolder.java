package we.devs.opium.client.commands;

import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.utilities.ChatUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;

@RegisterCommand(name="folder", description="Opens the Configuration folder.", syntax="folder", aliases={"openfolder", "cfgfldr", "cfgfolder", "configfolder"})
public class CommandFolder extends Command {
    @Override
    public void onCommand(String[] args) {
        try {
            Desktop.getDesktop().open(new File("Opium"));
            ChatUtils.sendMessage("Successfully opened configuration folder.", "Folder");
        }
        catch (IOException exception) {
            ChatUtils.sendMessage("Could not open configuration folder.", "Folder");
            exception.printStackTrace();
        }
    }
}