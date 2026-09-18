package hack.echo.client.handlers.impl;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.SprintEvent;
import hack.echo.client.handlers.Handler;
import lombok.Getter;
import net.minecraft.client.ToggleKeyMapping;

public class SprintController extends Handler {
    
    @Getter
    private static boolean forceSprint = false;
    @Getter
    private static boolean forceStopSprint = false;

    public static void setForceSprint(boolean value) {
        forceSprint = value;
        if (value && mc.player != null) {
            if (mc.player.isSprinting()) return;
            mc.player.setSprinting(true);
            setSprintKeyState(true);
        }
    }

    public static void setForceStopSprint(boolean value) {
        forceStopSprint = value;
        if (value && mc.player != null) {
            if (!mc.player.isSprinting()) return;
            mc.player.setSprinting(false);
            setSprintKeyState(false);
        }
    }

    private static void setSprintKeyState(boolean desired) {
        if (mc.options == null) {
            return;
        }

        if (mc.options.keySprint instanceof ToggleKeyMapping && mc.options.toggleSprint().get()) {
            if (mc.options.keySprint.isDown() != desired) {
                mc.options.keySprint.setDown(true);
            }
            return;
        }

        mc.options.keySprint.setDown(desired);
    }

    public static void reset() {
        forceSprint = false;
        forceStopSprint = false;
    }
    
    @EventSubscribe
    public void onSprint(SprintEvent event) {
        if (forceStopSprint) {
            event.setShouldSprint(false);
            return;
        }
        
        if (forceSprint) {
            event.setShouldSprint(true);
        }
    }
}
