package wtf.opal.client.socket.packet.impl.c2s;

import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferWriter;

@NativeInclude
public final class C2SCrashPacket implements C2SPacket {

    private final String username;

    public C2SCrashPacket(final String username) {
        this.username = username.length() > 24 ? username.substring(0, 24) : username;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(this.username);
    }

    @Override
    public int id() {
        return 12;
    }
}
