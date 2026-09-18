package com.instrumentalist.krs.events.features;

import com.instrumentalist.krs.events.EventArgument;
import com.instrumentalist.krs.events.EventListener;
import java.util.Objects;
import net.minecraft.network.protocol.Packet;

public class ReceivedPacketEvent extends EventArgument {
    public final Packet<?> packet;

    public ReceivedPacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onReceivedPacket(this);
    }
}