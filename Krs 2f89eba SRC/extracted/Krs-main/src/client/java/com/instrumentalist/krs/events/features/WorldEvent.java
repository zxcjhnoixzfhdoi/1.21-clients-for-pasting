package com.instrumentalist.krs.events.features;

import com.instrumentalist.krs.events.EventArgument;
import com.instrumentalist.krs.events.EventListener;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class WorldEvent extends EventArgument {

    public final @Nullable ClientLevel previousWorld;
    public final @Nullable ClientLevel world;

    public WorldEvent() {
        this(null, null);
    }

    public WorldEvent(@Nullable ClientLevel previousWorld, @Nullable ClientLevel world) {
        this.previousWorld = previousWorld;
        this.world = world;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onWorld(this);
    }
}
