package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;

/**
 * Fired from the client main loop every time the game processes a system update.
 * This is the highest frequency event you can reliably receive on the client thread.
 */
public class EventSystemUpdate extends Event {
    private final boolean shouldRunGameTick;
    @Getter
    private final float dynamicDeltaTicks;
    @Getter
    private final float tickProgress;
    @Getter
    private final float fixedDeltaTicks;

    public EventSystemUpdate(boolean shouldRunGameTick, float dynamicDeltaTicks, float tickProgress, float fixedDeltaTicks) {
        this.shouldRunGameTick = shouldRunGameTick;
        this.dynamicDeltaTicks = dynamicDeltaTicks;
        this.tickProgress = tickProgress;
        this.fixedDeltaTicks = fixedDeltaTicks;
    }

    public boolean shouldRunGameTick() {
        return shouldRunGameTick;
    }
}
