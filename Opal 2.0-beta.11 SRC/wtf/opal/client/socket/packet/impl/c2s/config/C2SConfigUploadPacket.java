package wtf.opal.client.socket.packet.impl.c2s.config;

import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferWriter;

@NativeInclude
public final class C2SConfigUploadPacket implements C2SPacket {

    private final String configName, configData;

    public C2SConfigUploadPacket(final String configName, final String configData) {
        this.configName = configName;
        this.configData = configData;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(configName);
        writer.writeString(configData);
    }

    @Override
    public int id() {
        return 9;
    }

}
