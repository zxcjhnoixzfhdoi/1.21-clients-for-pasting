package we.devs.opium.api.manager.music;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Identifier;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicStateManager {
    private static boolean isPlayingCustomMusic = false;
    private static final List<String> CUSTOM_MUSIC_TRACKS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static SoundInstance currentSongInstance;
    private static String lastPlayedTrack = null; // Saves last played track

    static {
        // Load all .ogg files from the mod_music folder
        loadMusicTracks();
    }

    /**
     * Loads all .ogg files from the mod_music folder.
     */
    public static void loadMusicTracks() {
        Path gameDir = Paths.get("").toAbsolutePath(); // Adjust this to your Minecraft game directory
        Path musicFolder = gameDir.resolve("opium/mod_music");

        if (musicFolder.toFile().exists()) {
            File[] files = musicFolder.toFile().listFiles((dir, name) -> name.endsWith(".ogg"));
            if (files != null) {
                for (File file : files) {
                    CUSTOM_MUSIC_TRACKS.add(file.getName());
                }
            }
        }
    }

    public static boolean isPlayingCustomMusic() {
        return isPlayingCustomMusic;
    }

    public static void setPlayingCustomMusic(boolean playing) {
        isPlayingCustomMusic = playing;
    }

    public static String getRandomMusicTrack() {
        if (CUSTOM_MUSIC_TRACKS.isEmpty()) {
            throw new IllegalStateException("No music tracks found in mod_music folder.");
        }

        String nextTrack;

        // Make sure the new song is different from the last played song
        do {
            nextTrack = CUSTOM_MUSIC_TRACKS.get(RANDOM.nextInt(CUSTOM_MUSIC_TRACKS.size()));
        } while (nextTrack.equals(lastPlayedTrack) && CUSTOM_MUSIC_TRACKS.size() > 1);

        lastPlayedTrack = nextTrack; // Update the last played track to the new one

        return nextTrack;
    }

    public static boolean doesExist(String trackName) {
        if (CUSTOM_MUSIC_TRACKS.isEmpty()) {return false;}
        return CUSTOM_MUSIC_TRACKS.contains(trackName);
    }
}