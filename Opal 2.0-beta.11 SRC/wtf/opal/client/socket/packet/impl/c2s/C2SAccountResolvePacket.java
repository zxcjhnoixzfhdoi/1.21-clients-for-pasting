package wtf.opal.client.socket.packet.impl.c2s;

import com.mojang.util.UndashedUuid;
import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferWriter;

import java.util.UUID;

@NativeInclude
public final class C2SAccountResolvePacket implements C2SPacket {

    private final UUID uuid;

    public C2SAccountResolvePacket(final UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(UndashedUuid.toString(this.uuid));
    }

    @Override
    public int id() {
        return 5;
    }

}
