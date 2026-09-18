package wtf.opal.client.socket.packet.impl.s2c;

import com.mojang.util.UndashedUuid;
import wtf.opal.client.feature.module.impl.visual.CapeModule;
import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferReader;
import wtf.opal.utility.socket.user.ResolvedUser;
import wtf.opal.utility.socket.user.UserRole;

import java.util.UUID;

@NativeInclude
public final class S2CAccountResolvePacket implements S2CPacket {

    private final UUID uuid;
    private final ResolvedUser user;

    public S2CAccountResolvePacket(final BufferReader reader) throws Exception {
        this.uuid = UndashedUuid.fromString(reader.readString());
        this.user = new ResolvedUser(
                reader.readString(),
                UserRole.fromName(reader.readString()),
                CapeModule.CapeType.fromSlug(reader.readString())
        );
    }

    @Override
    public void handle() {
        ClientSocket.getInstance().getUserCache().getResolvedUsers().put(this.uuid, this.user);
    }

    @Override
    public int id() {
        return 5;
    }

}
