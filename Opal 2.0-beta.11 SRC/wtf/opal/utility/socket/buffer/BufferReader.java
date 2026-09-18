package wtf.opal.utility.socket.buffer;

import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.CipherUtility;
import wtf.opal.utility.socket.EncryptionContext;

import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;

@NativeInclude
public final class BufferReader {

    private final DataInputStream stream;
    private final EncryptionContext ectx;

    public BufferReader(final DataInputStream stream, final EncryptionContext ectx) {
        this.stream = stream;
        this.ectx = ectx;
    }

    public int readInt() throws Exception {
//        return this.stream.readInt();
        return Integer.parseInt(this.readString());
    }

    public long readLong() throws Exception {
//        return this.stream.readLong();
        return Long.parseLong(this.readString());
    }

    public boolean readBoolean() throws Exception {
//        return this.stream.readBoolean();
        return Boolean.parseBoolean(this.readString());
    }

    public String readString(final boolean decrypt) throws Exception {
        final int len = this.stream.readInt();

        final byte[] bytes = new byte[len];
        this.stream.readFully(bytes);

        String value = new String(bytes, StandardCharsets.UTF_8);

        if (decrypt) {
            // layer 2
            if (ectx != null) {
                value = CipherUtility.aesDecrypt(value, ectx.aesKey());
            }

            // layer 1
            value = CipherUtility.decryptWithPassphrase(value, "net.raphimc.viabedrock.ViaBedrockConfig\u200B".toCharArray());
        }

        return value;
    }

    public String readString() throws Exception {
        return this.readString(true);
    }

}
