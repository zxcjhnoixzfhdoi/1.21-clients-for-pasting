package wtf.opal.client.socket.packet.api;

public interface ServerPacketConsumer {
    void accept(final S2CPacket packet);
}
