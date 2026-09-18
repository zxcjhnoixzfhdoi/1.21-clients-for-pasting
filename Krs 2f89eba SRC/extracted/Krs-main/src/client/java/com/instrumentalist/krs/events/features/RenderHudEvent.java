package com.instrumentalist.krs.events.features;

import com.instrumentalist.krs.events.EventArgument;
import com.instrumentalist.krs.events.EventListener;

import java.util.Objects;

public class RenderHudEvent extends EventArgument {
    public final float tickDelta;

    public RenderHudEvent(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onRenderHud(this);
    }
}
