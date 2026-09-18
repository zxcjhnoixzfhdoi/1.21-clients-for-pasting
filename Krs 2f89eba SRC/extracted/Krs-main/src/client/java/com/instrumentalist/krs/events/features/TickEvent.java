package com.instrumentalist.krs.events.features;

import com.instrumentalist.krs.events.EventArgument;
import com.instrumentalist.krs.events.EventListener;

import java.util.Objects;

public class TickEvent extends EventArgument {

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onTick(this);
    }
}
