package com.instrumentalist.krs.hacks.features.player;

import com.instrumentalist.krs.events.features.UpdateEvent;
import com.instrumentalist.krs.hacks.Module;
import com.instrumentalist.krs.hacks.ModuleCategory;
import com.instrumentalist.krs.utils.packet.BlinkUtil;
import org.lwjgl.glfw.GLFW;

public class Blink extends Module {

    public Blink() {
        super("Blink", ModuleCategory.Player, GLFW.GLFW_KEY_UNKNOWN, false, true);
    }

    @Override
    public String tag() {
        return BlinkUtil.INSTANCE.getPacketCount() + "ms";
    }

    @Override
    public void onDisable() {
        BlinkUtil.INSTANCE.sync(true, true);
        BlinkUtil.INSTANCE.stopBlink();
    }

    @Override
    public void onEnable() {
        BlinkUtil.INSTANCE.doBlink();
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        BlinkUtil.INSTANCE.doBlink();
    }
}
