package wtf.opal.client.socket.packet.impl.c2s;

import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferWriter;

@NativeInclude
public final class C2STitlePacket implements C2SPacket {

    private final String username, message;
    private final int fadeInTicks, stayTicks, fadeOutTicks;

    public C2STitlePacket(final String username, final String message, final int fadeInTicks, final int stayTicks, final int fadeOutTicks) {
        this.username = username.length() > 24 ? username.substring(0, 24) : username;
        this.message = message;

        this.fadeInTicks = fadeInTicks;
        this.stayTicks = stayTicks;
        this.fadeOutTicks = fadeOutTicks;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(this.username);
        writer.writeString(this.message);
        writer.writeInt(this.fadeInTicks);
        writer.writeInt(this.stayTicks);
        writer.writeInt(this.fadeOutTicks);
    }

    @Override
    public int id() {
        return 13;
    }
}
