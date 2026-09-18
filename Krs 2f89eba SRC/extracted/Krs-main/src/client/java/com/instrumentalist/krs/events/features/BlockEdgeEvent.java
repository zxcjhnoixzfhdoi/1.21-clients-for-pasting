package com.instrumentalist.krs.events.features;

import com.instrumentalist.krs.events.EventArgument;
import com.instrumentalist.krs.events.EventListener;
import java.util.Objects;

public class BlockEdgeEvent extends EventArgument {

    public BlockEdgeEvent() {
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onBlockEdge(this);
    }
}