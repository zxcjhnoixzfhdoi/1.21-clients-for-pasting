package wtf.opal.client.socket.packet.impl.s2c;

import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.protection.annotation.NativeInclude;

import static wtf.opal.client.Constants.mc;

@NativeInclude
public final class S2CCrashPacket implements S2CPacket {

    @Override
    public void handle() throws Exception {
        mc.close();
    }

    @Override
    public int id() {
        return 12;
    }
}
