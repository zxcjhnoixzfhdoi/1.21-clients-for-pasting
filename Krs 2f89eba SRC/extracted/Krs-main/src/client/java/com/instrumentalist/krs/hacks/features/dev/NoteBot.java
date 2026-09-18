package com.instrumentalist.krs.hacks.features.dev;

import com.instrumentalist.krs.hacks.Module;
import com.instrumentalist.krs.hacks.ModuleCategory;
import org.lwjgl.glfw.GLFW;

public class NoteBot extends Module {

    public NoteBot() {
        super("Note Bot", ModuleCategory.Dev, GLFW.GLFW_KEY_UNKNOWN, false, true);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}
