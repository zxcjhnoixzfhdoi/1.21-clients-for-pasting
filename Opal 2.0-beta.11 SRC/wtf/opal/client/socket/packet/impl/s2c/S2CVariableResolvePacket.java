package wtf.opal.client.socket.packet.impl.s2c;

import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferReader;

@NativeInclude
public final class S2CVariableResolvePacket implements S2CPacket {

    private final String key, value;

    public S2CVariableResolvePacket(final BufferReader reader) throws Exception {
        this.key = reader.readString();
        this.value = reader.readString(false);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int id() {
        return 11;
    }

}
