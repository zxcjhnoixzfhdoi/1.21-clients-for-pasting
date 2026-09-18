package wtf.opal.client.socket.packet.impl.c2s;

import com.mojang.util.UndashedUuid;
import org.jetbrains.annotations.NotNull;
import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferWriter;

import java.util.UUID;

// The socket server uses Mojang's session API to verify the user's identity to prevent spoofing
@NativeInclude
public final class C2SAccountUpdatePacket implements C2SPacket {

    private final UUID profileUUID;
    private final String accessToken, capeSlug;

    public C2SAccountUpdatePacket(final @NotNull UUID profileUUID, final @NotNull String accessToken, final String capeSlug) {
        this.profileUUID = profileUUID;
        this.accessToken = accessToken;
        this.capeSlug = capeSlug;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeString(UndashedUuid.toString(this.profileUUID));
        writer.writeString(this.accessToken);
        writer.writeString(this.capeSlug == null ? "" : this.capeSlug);
    }

    @Override
    public int id() {
        return 4;
    }

}
