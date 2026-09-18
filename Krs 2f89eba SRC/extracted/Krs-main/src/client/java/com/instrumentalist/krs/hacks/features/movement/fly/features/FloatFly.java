package com.instrumentalist.krs.hacks.features.movement.fly.features;



import com.instrumentalist.krs.events.features.*;
import com.instrumentalist.krs.hacks.features.movement.fly.FlyEvent;
import com.instrumentalist.krs.utils.move.MovementUtil;

public class FloatFly implements FlyEvent {

    @Override
    public String getName() {
        return "Float";
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        MovementUtil.setVelocityY(0.0);
    }

    @Override
    public void onMotion(MotionEvent event) {
    }

    @Override
    public void onTick(TickEvent event) {
    }

    @Override
    public void onSendPacket(SendPacketEvent event) {
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
    }

    @Override
    public void onBlock(BlockEvent event) {
    }
}