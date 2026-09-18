package we.devs.opium.api.manager.music;

public class CustomSoundEngineManager {
    private static CustomSoundEngine soundEngine;

    public static CustomSoundEngine getSoundEngine() {
        if (soundEngine == null) {
            soundEngine = new CustomSoundEngine();
            soundEngine.init();
        }
        return soundEngine;
    }

    public static void stopMusic() {
        if (soundEngine != null) {
            soundEngine.stop();
        }
    }

    public static void cleanup() {
        if (soundEngine != null) {
            soundEngine.cleanup();
            soundEngine = null;
        }
    }
}