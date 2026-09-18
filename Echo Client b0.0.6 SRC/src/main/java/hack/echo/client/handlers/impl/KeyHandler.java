package hack.echo.client.handlers.impl;

import org.lwjgl.glfw.GLFW;

import hack.echo.client.Echo;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventKey;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Feature;
import hack.echo.client.handlers.Handler;
import hack.echo.client.handlers.InputHandler;
import net.minecraft.client.Minecraft;

import java.util.HashMap;


public class KeyHandler extends Handler {
    @EventSubscribe
    public void onKey(EventKey event) {
        // Needed for JNT
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null)
            return;

        if (event.getAction() != GLFW.GLFW_PRESS)
            return;

        for (Feature feature : Echo.featureManager.getFeatures()) {
            if (feature.getKey() != -1 && feature.getKey() == event.getKey()) {
                feature.toggle();
            }
        }
    }

    private final HashMap<Integer, Boolean> prevMouseState = new HashMap<>();

    @EventSubscribe
    public void onTick(EventTick tick) {
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        HashMap<Integer, Boolean> pressedThisTick = new HashMap<>();
        HashMap<Integer, Boolean> downThisTick = new HashMap<>();

        for (Feature feature : Echo.featureManager.getFeatures()) {
            int key = feature.getKey();
            if (key == -1) continue;
            if ((key & 0x80000000) == 0) continue;

            int mb = key & 0xFF;
            boolean pressed = pressedThisTick.computeIfAbsent(mb, b -> {
                boolean down = InputHandler.isMouseDown(b);
                downThisTick.put(b, down);
                return down && !prevMouseState.getOrDefault(b, false);
            });

            if (pressed) {
                feature.toggle();
            }
        }

        for (var entry : downThisTick.entrySet()) {
            prevMouseState.put(entry.getKey(), entry.getValue());
        }
    }
}
