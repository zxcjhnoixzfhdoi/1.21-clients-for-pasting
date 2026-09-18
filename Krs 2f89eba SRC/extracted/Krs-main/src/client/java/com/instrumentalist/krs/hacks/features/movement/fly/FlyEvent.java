package com.instrumentalist.krs.hacks.features.movement.fly;

import com.instrumentalist.krs.events.features.*;
import com.instrumentalist.krs.utils.IMinecraft;

public interface FlyEvent extends IMinecraft {
    String getName();
    void onUpdate(UpdateEvent event);
    void onMotion(MotionEvent event);
    void onTick(TickEvent event);
    void onSendPacket(SendPacketEvent event);
    void onReceivedPacket(ReceivedPacketEvent event);
    void onBlock(BlockEvent event);
}
