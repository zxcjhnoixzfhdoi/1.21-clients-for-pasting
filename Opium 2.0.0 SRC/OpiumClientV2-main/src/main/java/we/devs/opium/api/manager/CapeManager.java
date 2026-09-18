package we.devs.opium.api.manager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import we.devs.opium.api.utilities.ChatUtils;

import java.util.ArrayList;
import java.util.List;

public class CapeManager {
    private static Identifier currentCape = Identifier.of("opium", "textures/capes/cape.png");
    private static List<Identifier> animatedCapeFrames = new ArrayList<>();
    private static int currentFrame = 0;
    private static long lastFrameTime = 0;
    private static final long FRAME_DELAY = 100; // Delay between frames in milliseconds

    public static Identifier getCape() {
        if (!animatedCapeFrames.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime >= FRAME_DELAY) {
                currentFrame = (currentFrame + 1) % animatedCapeFrames.size();
                lastFrameTime = currentTime;
            }
            return animatedCapeFrames.get(currentFrame);
        }
        return currentCape;
    }

    public static void setCurrentCape(Identifier cape) {
        currentCape = cape;
        animatedCapeFrames.clear(); // Stop animation when switching to a static cape
    }

    public static void setAnimatedCape(String animationName) {
        animatedCapeFrames = loadAnimationFrames(animationName);
        currentFrame = 0;
        lastFrameTime = System.currentTimeMillis();
    }

    public static List<Identifier> loadAnimationFrames(String animationName) {
        List<Identifier> frames = new ArrayList<>();
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

        // Look for frames in the folder
        int frameCount = 1;
        while (true) {
            Identifier frameLocation = Identifier.of("opium", "textures/capes/animated_cape/" + animationName + "/frame_" + frameCount + ".png");
            if (resourceManager.getResource(frameLocation).isEmpty()) {
                break; // Stop if no more frames are found
            }
            frames.add(frameLocation);
            frameCount++;
        }

        return frames;
    }

    public static void setCape(String capeName) {
        Identifier newCape = Identifier.of("opium", "textures/capes/" + capeName + ".png");
        CapeManager.setCurrentCape(newCape);
    }

    public static void setFullAnimatedCape(String animationName) {
        List<Identifier> frames = CapeManager.loadAnimationFrames(animationName);
        if (frames.isEmpty()) {
            ChatUtils.sendMessage("No animated cape found for animation " + animationName, "Cape");
            return;
        }
        CapeManager.setAnimatedCape(animationName);
    }
}