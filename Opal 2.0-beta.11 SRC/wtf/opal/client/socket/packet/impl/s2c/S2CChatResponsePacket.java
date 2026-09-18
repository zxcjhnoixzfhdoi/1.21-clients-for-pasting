package wtf.opal.client.socket.packet.impl.s2c;

import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.client.socket.packet.types.ChatResponseType;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.misc.chat.ChatUtility;
import wtf.opal.utility.socket.buffer.BufferReader;

@NativeInclude
public final class S2CChatResponsePacket implements S2CPacket {

    private final int responseType;
    private final String message;

    public S2CChatResponsePacket(final BufferReader reader) throws Exception {
        this.responseType = reader.readInt();
        this.message = reader.readString();
    }

    @Override
    public void handle() {
        switch (responseType) {
            case ChatResponseType.ERROR -> ChatUtility.error(message);
            case ChatResponseType.SUCCESS -> ChatUtility.success(message);
        }
    }

    @Override
    public int id() {
        return 9;
    }

}
