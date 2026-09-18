package com.instrumentalist.krs.hacks.features.dev;

import com.instrumentalist.krs.events.features.*;
import com.instrumentalist.krs.hacks.Module;
import com.instrumentalist.krs.hacks.ModuleCategory;
import com.instrumentalist.krs.utils.ChatUtil;
import com.instrumentalist.krs.utils.packet.PacketUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import org.lwjgl.glfw.GLFW;

public class TuckMod extends Module {

    public TuckMod() {
        super("Tuck Mod", ModuleCategory.Dev, GLFW.GLFW_KEY_UNKNOWN, false, true);
    }

    private int tick;
    private int tick2;
    private int tick3;
    private int tick4;

    @Override
    public void onDisable() {
        tick = 0;
        tick2 = 0;
        tick3 = 0;
        tick4 = 0;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onUpdate(UpdateEvent event) {
    }

    @Override
    public void onMotion(MotionEvent event) {
    }

    @Override
    public void onTick(TickEvent event) {
    }

    @Override
    public void onAttack(AttackEvent event) {
    }

    @Override
    public void onSendPacket(SendPacketEvent event) {
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
        if (mc.player == null) return;

        Packet<?> packet = event.packet;

        if (packet instanceof ClientboundKeepAlivePacket ping) {
            ChatUtil.printChat(ping.getId() + "");
            event.cancel();
            PacketUtil.sendPacketAsSilent(new ServerboundKeepAlivePacket(Integer.MIN_VALUE));
        }
    }
}
