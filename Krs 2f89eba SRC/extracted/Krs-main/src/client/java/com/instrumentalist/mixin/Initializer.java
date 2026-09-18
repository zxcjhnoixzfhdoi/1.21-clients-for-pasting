package com.instrumentalist.mixin;

import com.instrumentalist.krs.Client;
import imgui.ImGui;
import net.fabricmc.api.ClientModInitializer;
import xyz.breadloaf.imguimc.interfaces.Renderable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Initializer implements ClientModInitializer {
    public static final List<Renderable> renderstack = new CopyOnWriteArrayList<>();

    @Override
    public void onInitializeClient() {
        Client.mixinInitializeHook();
    }

    public static void pushRenderable(Renderable renderable) {
        renderstack.add(renderable);
    }

    public static void pullEveryRenderable() {
        renderstack.clear();
    }

    public static int getDockId() {
        return ImGui.getID("imgui-mc dockspace");
    }
}
