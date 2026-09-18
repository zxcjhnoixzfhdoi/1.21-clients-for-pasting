package com.instrumentalist.krs.events.features;

import com.instrumentalist.krs.events.EventArgument;
import com.instrumentalist.krs.events.EventListener;

import java.util.Objects;

public class MouseScrollEvent extends EventArgument {
    public final double horizontal;
    public final double vertical;

    public MouseScrollEvent(double horizontal, double vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onMouseScroll(this);
    }
}