package wtf.opal.client.socket.packet.impl.c2s;

import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.buffer.BufferWriter;
import wtf.opal.utility.socket.buffer.BufferWriterConsumer;

@NativeInclude
public final class C2SKeepAlivePacket implements C2SPacket {

    private final BufferWriterConsumer challenge;

    public C2SKeepAlivePacket(final BufferWriterConsumer challenge) {
        this.challenge = challenge;
    }

    @Override
    public void serialize(final BufferWriter writer) throws Exception {
        writer.writeInt(ClientSocket.getInstance().getKeepAliveCount()); // client count
        this.challenge.accept(writer);
    }

    @Override
    public int id() {
        return 0;
    }

}
