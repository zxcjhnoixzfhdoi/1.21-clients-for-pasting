package wtf.opal.client.socket.packet.impl.c2s;

import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferWriter;

@NativeInclude
public final class C2SVariableResolvePacket implements C2SPacket {

    private final String key;

    public C2SVariableResolvePacket(final String key) {
        this.key = key;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(this.key);
    }

    @Override
    public int id() {
        return 11;
    }

}
