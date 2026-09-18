package wtf.opal.utility.socket.buffer;

public interface BufferWriterConsumer {
    void accept(final BufferWriter writer) throws Exception;
}
