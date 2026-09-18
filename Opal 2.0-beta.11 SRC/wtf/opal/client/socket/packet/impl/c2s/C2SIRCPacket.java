package wtf.opal.client.socket.packet.impl.c2s;

import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.client.socket.packet.types.IRCPacketType;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferWriter;

@NativeInclude
public final class C2SIRCPacket implements C2SPacket {

    private final int packetType;
    private String username, message;

    public C2SIRCPacket(final int packetType, final String message) {
        if (packetType != IRCPacketType.BROADCAST) {
            throw new IllegalArgumentException("Invalid request type");
        }
        this.packetType = packetType;
        this.message = message.length() > 256 ? message.substring(0, 256) : message;
    }

    public C2SIRCPacket(final int packetType, final String username, final String message) {
        if (packetType != IRCPacketType.WHISPER_RECEIVED) {
            throw new IllegalArgumentException("Invalid request type");
        }
        this.packetType = packetType;
        this.username = username.length() > 24 ? username.substring(0, 24) : username;
        this.message = message.length() > 256 ? message.substring(0, 256) : message;
    }

    public C2SIRCPacket(final int packetType) {
        if (packetType != IRCPacketType.LIST_ONLINE) {
            throw new IllegalArgumentException("Invalid request type");
        }
        this.packetType = packetType;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeInt(this.packetType);

        switch (this.packetType) {
            case IRCPacketType.BROADCAST -> {
                writer.writeString(this.message);
            }
            case IRCPacketType.WHISPER_RECEIVED -> {
                writer.writeString(this.username);
                writer.writeString(this.message);
            }
        }
    }

    @Override
    public int id() {
        return 6;
    }

}
