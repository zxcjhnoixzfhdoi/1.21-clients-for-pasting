package we.devs.opium.client.commands;

import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.manager.music.CustomSoundEngine;
import we.devs.opium.api.manager.music.CustomSoundEngineManager;
import we.devs.opium.api.manager.music.MusicStateManager;
import we.devs.opium.api.utilities.ChatUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static we.devs.opium.api.manager.music.MusicStateManager.loadMusicTracks;


@RegisterCommand(name="play", description="Playes Music from Your Music Folder", syntax="play <name> / play stop / play folder", aliases={"pl", "play", "music"})
public class CommandPlayMusic extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 1) {
            if (args[0].equals("reload")) {
                loadMusicTracks();
                ChatUtils.sendMessage("Successfully reloaded", "Music");
            }
            else if (args[0].equals("folder")) {
                try {
                    Desktop.getDesktop().open(new File("Opium/mod_music"));
                    ChatUtils.sendMessage("Successfully opened music folder.", "Music");
                }
                catch (IOException exception) {
                    ChatUtils.sendMessage("Could not open music folder.", "Music");
                    exception.printStackTrace();
                }
            }
            else if (args[0].equals("stop") && MusicStateManager.isPlayingCustomMusic()) {
                CustomSoundEngineManager.stopMusic();
                MusicStateManager.setPlayingCustomMusic(false);
                ChatUtils.sendMessage("Stopped Music", "Music");
            }
            else {
                if (MusicStateManager.doesExist(args[0])) {
                    CustomSoundEngine.playMusic(args[0]);
                    MusicStateManager.setPlayingCustomMusic(true);
                    ChatUtils.sendMessage(args[0] + " is playing!", "Music");

                } else {ChatUtils.sendMessage(args[0] + " not found in folder (open folder with /pl folder)", "Music");}

            }

        } else {
            this.sendSyntax();
        }
    }
}
