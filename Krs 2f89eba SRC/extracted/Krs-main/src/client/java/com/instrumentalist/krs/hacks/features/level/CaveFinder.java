package com.instrumentalist.krs.hacks.features.level;

import com.instrumentalist.krs.hacks.Module;
import com.instrumentalist.krs.hacks.ModuleCategory;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.lwjgl.glfw.GLFW;

public class CaveFinder extends Module {
    private static volatile boolean active;

    public CaveFinder() {
        super("Cave Finder", ModuleCategory.Level, GLFW.GLFW_KEY_UNKNOWN, false, true);
    }

    @Override
    public void onEnable() {
        active = true;
        reloadChunks();
    }

    @Override
    public void onDisable() {
        active = false;
        reloadChunks();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean shouldCullFront(RenderPipeline pipeline) {
        if (!active || pipeline == null || pipeline.getLocation() == null)
            return false;

        String path = pipeline.getLocation().getPath();
        return path.endsWith("pipeline/solid_terrain")
                || path.endsWith("pipeline/cutout_terrain");
    }

    private void reloadChunks() {
        if (mc.level != null)
            mc.levelExtractor.allChanged();
    }
}
