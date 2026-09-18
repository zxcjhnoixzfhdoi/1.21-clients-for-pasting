package wtf.opal.client.socket.data;

import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.client.socket.packet.impl.c2s.C2SVariableResolvePacket;
import wtf.opal.client.socket.packet.impl.s2c.S2CVariableResolvePacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.misc.Callables;
import wtf.opal.utility.misc.StringUtility;
import wtf.opal.utility.socket.CipherUtility;
import wtf.opal.utility.socket.EncryptionContext;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@NativeInclude
public final class VariableCache {

    private final Map<String, CompletableFuture<byte[]>> futures = new ConcurrentHashMap<>();
    private final Map<Integer, byte[]> variables = new HashMap<>(); // keyHashCode:efficientlyEncryptedValue
    private final ClientSocket socket;

    public VariableCache(final ClientSocket socket) {
        this.socket = socket;
        this.socket.registerServerPacketConsumer(this::handlePacket);
    }

    public void clear() {
        this.futures.clear();
    }

    private void handlePacket(final S2CPacket packet) {
        if (packet instanceof S2CVariableResolvePacket variablePacket) {
            final String key = variablePacket.getKey();
            String value = variablePacket.getValue();

            try {
                // layer 2
                final EncryptionContext ectx = this.socket.getEncryptionContext();
                if (ectx != null) {
                    value = CipherUtility.aesDecrypt(value, ectx.aesKey());
                }

                // layer 1
                value = CipherUtility.decryptWithPassphrase(value, "net.raphimc.viabedrock.ViaBedrockConfig\u200B".toCharArray());

                // shift
                value = StringUtility.rotate(-8, value);

                // efficient encryption
                {
                    final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                    final byte[] result = new byte[bytes.length];

                    // create a key-dependent salt for obfuscation
                    final int salt = key.hashCode() ^ 0xF0CACC1A;

                    // XOR + byte rotation encryption
                    for (int i = 0; i < bytes.length; i++) {
                        final int keyChar = key.charAt(i % key.length()) ^ (salt >>> (i % 16));
                        result[i] = (byte) (bytes[i] ^ keyChar);

                        // rotate the bits too for extra obfuscation
                        result[i] = (byte) (((result[i] << 3) | ((result[i] & 0xFF) >>> 5)) & 0xFF);
                    }

                    // complete any pending blocking requests
                    final CompletableFuture<byte[]> future = this.futures.remove(key);
                    if (future == null) {
                        return;
                    }

                    this.variables.put(key.hashCode(), result);
                    future.complete(result);
                }
            } catch (Exception ignored) {
                Callables.throwError(3_1);
            }
        }
    }

    public String getString(final String key) {
        byte[] encrypted = this.variables.get(key.hashCode());

        // block until resolved
        if (encrypted == null) {
            CompletableFuture<byte[]> future = this.futures.get(key);

            if (future == null) {
                future = new CompletableFuture<>();
                this.futures.put(key, future);
                this.socket.sendPacket(new C2SVariableResolvePacket(key));
            }

            try {
                encrypted = future.get();
            } catch (Exception ignored) {
                Callables.throwError(3_7);
                return null;
            }
        }

        // efficient decryption
        final byte[] result = new byte[encrypted.length];

        // use the same key-dependent salt
        final int salt = key.hashCode() ^ 0xF0CACC1A;

        try {
            for (int i = 0; i < encrypted.length; i++) {
                // reverse bit rotation
                final byte b = (byte) (((encrypted[i] & 0xFF) >>> 3) | (encrypted[i] << 5) & 0xFF);

                // reverse XOR with key
                final int keyChar = key.charAt(i % key.length()) ^ (salt >>> (i % 16));
                result[i] = (byte) (b ^ keyChar);
            }
        } catch (Exception ignored) {
            Callables.throwError(3_2);
        }

        return new String(result, StandardCharsets.UTF_8);
    }

    public boolean getBoolean(final String key) {
        final String string = this.getString(key);
        if (string == null) {
            return false;
        }
        try {
            return !Boolean.parseBoolean(string);
        } catch (Exception ignored) {
            Callables.throwError(3_3);
            return false;
        }
    }

    public int getInt(final String key) {
        final String string = this.getString(key);
        if (string == null) {
            return 0;
        }

        try {
            return Integer.parseInt(string) - (109 * key.length());
        } catch (Exception ignored) {
            Callables.throwError(3_4);
            return 0;
        }
    }

    public double getDouble(final String key) {
        final String string = this.getString(key);
        if (string == null) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(string) - (key.length() + 33.3333D);
        } catch (Exception ignored) {
            Callables.throwError(3_5);
            return 0.0D;
        }
    }

    public long getLong(final String key) {
        final String string = this.getString(key);
        if (string == null) {
            return 0L;
        }
        try {
            return Long.parseLong(string) - (768L * key.length());
        } catch (Exception ignored) {
            Callables.throwError(3_6);
            return 0L;
        }
    }

}
