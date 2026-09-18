package wtf.opal.client.socket.packet.impl.s2c;

import wtf.opal.client.OpalClient;
import wtf.opal.client.ReleaseInfo;
import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.client.socket.packet.impl.c2s.C2SHandshakePacket;
import wtf.opal.client.socket.packet.types.HandshakeAuthStatus;
import wtf.opal.client.socket.packet.types.HandshakeStage;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.security.HWIDUtility;
import wtf.opal.utility.security.SessionUtility;
import wtf.opal.utility.socket.buffer.BufferReader;
import wtf.opal.utility.socket.client.MetadataUtility;
import wtf.opal.utility.socket.user.LocalUser;
import wtf.opal.utility.socket.user.UserRole;

@NativeInclude
public final class S2CHandshakePacket implements S2CPacket {

    private final int stage;
    private int authResponseType;

    private int numericId;
    private String userName, userRole;

    public S2CHandshakePacket(final BufferReader reader) throws Exception {
        this.stage = reader.readInt();
        if (this.stage == HandshakeStage.AUTH_RESPONSE) {
            this.authResponseType = reader.readInt();
            if (this.authResponseType == HandshakeAuthStatus.ACCEPTED) {
                this.numericId = reader.readInt();
                this.userName = reader.readString();
                this.userRole = reader.readString();
            }
        }
    }

    @Override
    public void handle() {
        final ClientSocket socket = ClientSocket.getInstance();

        switch (stage) {
            case HandshakeStage.READY -> {
                if (SessionUtility.getKeystoreToken() == null && !SessionUtility.requestHandoffAuthorizationOrStop()) {
                    return;
                }

                socket.sendPacket(new C2SHandshakePacket(
                        MetadataUtility.getAgent(), ReleaseInfo.CHANNEL.toString(),
                        SessionUtility.getKeystoreToken(), HWIDUtility.getHardwareId()
                ));
            }
            case HandshakeStage.AUTH_RESPONSE -> {
                switch (this.authResponseType) {
                    case HandshakeAuthStatus.INVALID_SESSION -> {
                        if (SessionUtility.requestHandoffAuthorizationOrStop()) {
                            socket.close();
                            socket.reconnect();
                        }
                    }
                    case HandshakeAuthStatus.ACCEPTED -> {
                        OpalClient.getInstance().setUser(new LocalUser(numericId, userName, UserRole.fromName(userRole)));
                        socket.setBlockMainThread(false);
                        socket.setAuthenticated(true);
                    }
                }
            }
        }
    }

    @Override
    public int id() {
        return 1;
    }

}
