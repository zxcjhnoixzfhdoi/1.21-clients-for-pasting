package wtf.opal.utility.socket.buffer;

import org.jetbrains.annotations.NotNull;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.socket.CipherUtility;
import wtf.opal.utility.socket.EncryptionContext;

import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

@NativeInclude
public final class BufferWriter {

    private final DataOutputStream stream;
    private final EncryptionContext ectx;

    public BufferWriter(final DataOutputStream stream, final EncryptionContext ectx) {
        this.stream = stream;
        this.ectx = ectx;
    }

    public void writeString(@NotNull String value) throws Exception {
        // layer 1
        value = CipherUtility.encryptWithPassphrase(value, "net.raphimc.viabedrock.ViaBedrockConfig\u200B".toCharArray());

        // layer 2
        if (this.ectx != null) {
            value = CipherUtility.aesEncrypt(value, this.ectx.aesKey(), CipherUtility.generateIV());
        }

        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        this.stream.writeInt(bytes.length);
        this.stream.write(bytes);
    }

    public void writeInt(final int value) throws Exception {
//        this.stream.writeInt(value);
        this.writeString(String.valueOf(value));
    }

    public void writeLong(final long value) throws Exception {
//        this.stream.writeLong(value);
        this.writeString(String.valueOf(value));
    }

    public void writeBoolean(final boolean value) throws Exception {
//        this.stream.writeBoolean(value);
        this.writeString(String.valueOf(value));
    }

}
