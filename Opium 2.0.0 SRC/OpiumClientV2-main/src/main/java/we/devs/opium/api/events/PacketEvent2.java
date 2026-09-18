package we.devs.opium.api.events;

import net.minecraft.network.packet.Packet;
import we.devs.opium.api.manager.event.Event;

public class PacketEvent2 extends Event2 {

    private final Packet<?> packet;
    public PacketEvent2(Packet<?> packet) {
        super(Stage.Pre);
        this.packet = packet;
    }
    public <T extends Packet<?>> T getPacket() {
        return (T) packet;
    }
    public static class Send extends PacketEvent2 {
        public Send(Packet<?> packet) {
            super(packet);
        }
    }

    public static class Receive extends PacketEvent2 {
        public Receive(Packet<?> packet) {
            super(packet);
        }

    }
}
