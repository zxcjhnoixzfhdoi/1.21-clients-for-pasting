package wtf.opal.client.socket.packet.impl.c2s.config;

import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferWriter;

@NativeInclude
public final class C2SConfigDeletePacket implements C2SPacket {

    private final String configName;

    public C2SConfigDeletePacket(final String configName) {
        this.configName = configName;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(configName);
    }

    @Override
    public int id() {
        return 10;
    }

}
