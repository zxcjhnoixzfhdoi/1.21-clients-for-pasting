package hack.echo.client.utils.audio;

import hack.echo.client.Echo;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.ResourceHelper;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class EchoAudio implements Imports {

    private record PcmData(ShortBuffer pcm, int channels, int sampleRate) {}
    private record PlayRequest(int bufferId, float volume, float pitch) {}

    private static final Map<String, String> SOUND_PATHS = Map.of(
        "click",    "sounds/clickgui/click.ogg",
        "toggle",   "sounds/clickgui/toggle.ogg",
        "hover",    "sounds/clickgui/hover.ogg",
        "keypress", "sounds/clickgui/keypress.ogg",
        "scroll",   "sounds/clickgui/scroll.ogg",
        "collapse", "sounds/clickgui/collapse.ogg"
    );

    private static final Map<String, PcmData>  pcmCache      = new HashMap<>();
    private static final Map<String, Integer>  alBuffers     = new HashMap<>();
    private static final List<Integer>         activeSources = new ArrayList<>();
    private static final ConcurrentLinkedQueue<PlayRequest> pending = new ConcurrentLinkedQueue<>();

    private static volatile boolean initialized = false;

    private EchoAudio() {}

    /** Decode OGGs files to PCM ShortBuffers. */
    public static void preload() {
        for (Map.Entry<String, String> e : SOUND_PATHS.entrySet()) {
            String name = e.getKey(), path = e.getValue();
            byte[] bytes = ResourceHelper.getBytes(path);
            if (bytes == null) { Echo.LOGGER.warn("[EchoAudio] Missing: " + path); continue; }

            ByteBuffer oggBuf = MemoryUtil.memAlloc(bytes.length);
            oggBuf.put(bytes).flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer errBuf = stack.mallocInt(1);
                long vorbis = STBVorbis.stb_vorbis_open_memory(oggBuf, errBuf, null);
                if (vorbis == 0) {
                    Echo.LOGGER.error("[EchoAudio] STBVorbis error " + errBuf.get(0) + " for " + name);
                    MemoryUtil.memFree(oggBuf);
                    continue;
                }

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(vorbis, info);
                int channels   = info.channels();
                int sampleRate = info.sample_rate();

                int samples = STBVorbis.stb_vorbis_stream_length_in_samples(vorbis);
                ShortBuffer pcm = MemoryUtil.memAllocShort(samples * channels);
                STBVorbis.stb_vorbis_get_samples_short_interleaved(vorbis, channels, pcm);
                STBVorbis.stb_vorbis_close(vorbis);
                pcmCache.put(name, new PcmData(pcm, channels, sampleRate));
            }
            MemoryUtil.memFree(oggBuf);
        }
    }

    /** Upload PCM to OpenAL buffers. MUST run on sound engine thread. */
    public static void initBuffers() {
        if (initialized) return;
        initialized = true;
        for (Map.Entry<String, PcmData> e : pcmCache.entrySet()) {
            PcmData d = e.getValue();
            int[] buf = new int[1];
            AL10.alGenBuffers(buf);
            int fmt = d.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            d.pcm().rewind();
            AL10.alBufferData(buf[0], fmt, d.pcm(), d.sampleRate());
            alBuffers.put(e.getKey(), buf[0]);
        }
    }

    /** Reset + reinit (called after SoundEngine.reload()). */
    public static void reinitBuffers() {
        for (int src : activeSources) { AL10.alSourceStop(src); AL10.alDeleteSources(src); }
        activeSources.clear();
        pending.clear();
        for (int id : alBuffers.values()) AL10.alDeleteBuffers(id);
        alBuffers.clear();
        initialized = false;
        initBuffers();
    }

    public static void play(String name, float volume, float pitch) {
        if (!initialized) return;

        Integer id = alBuffers.get(name);
        if (id == null) return;

        float masterVolume = mc.options.getSoundSourceVolume(SoundSource.MASTER);
        float uiVolume = mc.options.getSoundSourceVolume(SoundSource.UI);
        float finalVolume = volume * masterVolume * uiVolume;

        if (finalVolume <= 0f) return;
        pending.offer(new PlayRequest(id, finalVolume, pitch));
    }

    /** Drain queue + cleanup. MUST run on sound engine thread. */
    public static void tick() {
        PlayRequest req;
        while ((req = pending.poll()) != null) {
            int[] src = new int[1];
            AL10.alGenSources(src);
            if (src[0] == 0 || AL10.alGetError() != AL10.AL_NO_ERROR) continue;
            AL10.alSourcei(src[0], AL10.AL_BUFFER,          req.bufferId());
            AL10.alSourcef(src[0], AL10.AL_GAIN,            req.volume());
            AL10.alSourcef(src[0], AL10.AL_PITCH,           req.pitch());
            AL10.alSourcei(src[0], AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(src[0], AL10.AL_POSITION, 0f, 0f, 0f);
            AL10.alSourcePlay(src[0]);
            activeSources.add(src[0]);
        }
        activeSources.removeIf(src -> {
            int state = AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_STOPPED || state == AL10.AL_INITIAL) {
                AL10.alDeleteSources(src);
                return true;
            }
            return false;
        });
    }

    public static void destroy() {
        for (int src : activeSources) { AL10.alSourceStop(src); AL10.alDeleteSources(src); }
        activeSources.clear();
        for (int id : alBuffers.values()) AL10.alDeleteBuffers(id);
        alBuffers.clear();
        for (PcmData d : pcmCache.values()) MemoryUtil.memFree(d.pcm());
        pcmCache.clear();
        initialized = false;
    }
}
