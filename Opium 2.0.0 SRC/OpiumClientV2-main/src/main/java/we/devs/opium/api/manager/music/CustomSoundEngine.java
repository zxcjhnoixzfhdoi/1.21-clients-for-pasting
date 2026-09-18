package we.devs.opium.api.manager.music;

import net.minecraft.client.option.GameOptions;
import net.minecraft.sound.SoundCategory;
import org.lwjgl.openal.*;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import we.devs.opium.Opium;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static we.devs.opium.Opium.mc;

public class CustomSoundEngine {

    private long device;
    private long context;
    private int source;
    private int buffer;
    private boolean isPlaying = false;
    private String currentTrackName;
    private static Logger LOGGER;

    /**
     * Initializes the OpenAL sound engine.
     */
    public void init() {
        // Initialize OpenAL device and context
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (device == MemoryUtil.NULL) {
            if (LOGGER != null) LOGGER.error("Failed to open OpenAL device.");
        }

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        if (context == MemoryUtil.NULL) {
            if (LOGGER != null) LOGGER.error("Failed to open OpenAL context.");
        }

        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(alcCapabilities);
    }

    /**
     * Loads a sound file from the given path.
     *
     * @param filePath The path to the .ogg file.
     */
    public void loadSound(String filePath) {
        this.currentTrackName = Paths.get(filePath).getFileName().toString().replaceFirst("\\.ogg$", "");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            ShortBuffer rawAudioBuffer = STBVorbis.stb_vorbis_decode_filename(filePath, channels, sampleRate);

            if (rawAudioBuffer == null) {
                if (LOGGER != null) LOGGER.error("Failed to load sound file: {}", filePath);
            }

            // Generate a buffer and upload the sound data
            buffer = AL10.alGenBuffers();
            assert rawAudioBuffer != null;
            AL10.alBufferData(buffer, channels.get(0) == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, rawAudioBuffer, sampleRate.get(0));

            // Generate a source and attach the buffer
            source = AL10.alGenSources();
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
        }
    }

    /**
     * Plays the loaded sound with Minecraft's music volume.
     */
    public void play() {
        if (source == 0) {
            if (LOGGER != null) LOGGER.error("No sound loaded. Call loadSound() first.");
        }

        // Get Minecraft's music volume setting
        float musicVolume = mc.options.getSoundVolume(SoundCategory.MUSIC);

        // Set the volume of the OpenAL source
        AL10.alSourcef(source, AL10.AL_GAIN, musicVolume);

        // Play the sound
        AL10.alSourcePlay(source);
        isPlaying = true;
    }

    /**
     * Stops the currently playing sound.
     */
    public void stop() {
        if (source != 0) {
            AL10.alSourceStop(source);
            isPlaying = false;
        }
    }

    /**
     * Checks if the sound is currently playing.
     */
    public boolean isPlaying() {
        if (source != 0) {
            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            isPlaying = state == AL10.AL_PLAYING;
        }
        return isPlaying;
    }

    /**
     * Cleans up OpenAL resources.
     */
    public void cleanup() {
        if (source != 0) {
            AL10.alDeleteSources(source);
            source = 0;
        }
        if (buffer != 0) {
            AL10.alDeleteBuffers(buffer);
            buffer = 0;
        }
        if (context != MemoryUtil.NULL) {
            ALC10.alcDestroyContext(context);
            context = MemoryUtil.NULL;
        }
        if (device != MemoryUtil.NULL) {
            ALC10.alcCloseDevice(device);
            device = MemoryUtil.NULL;
        }
    }


    public String getCurrentTrackName() {
        return currentTrackName;
    }

    /**
     * Utility method to play a sound file directly with Minecraft's music volume.
     *
     * @param fileName    The name of the .ogg file in the "music" folder.
     */
    public static void playMusic(String fileName) {
        Path gameDir = Paths.get("").toAbsolutePath(); // Adjust this to your Minecraft game directory
        Path musicFolder = gameDir.resolve("opium/mod_music");
        Path soundFile = musicFolder.resolve(fileName);

        CustomSoundEngine soundEngine = new CustomSoundEngine();
        soundEngine.init();
        soundEngine.loadSound(soundFile.toString());
        soundEngine.play();

        // Use Minecraft's tick system to manage playback instead of Thread.sleep()
    }
}