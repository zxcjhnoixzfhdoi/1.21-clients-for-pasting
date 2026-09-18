package wtf.opal.client.socket.packet.impl.s2c;

import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.client.socket.packet.impl.c2s.C2SUpgradePacket;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.misc.StringUtility;
import wtf.opal.utility.socket.EncryptionContext;
import wtf.opal.utility.socket.buffer.BufferReader;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@NativeInclude
public final class S2CUpgradePacket implements S2CPacket {

    private final String publicKey;

    public S2CUpgradePacket(final BufferReader reader) throws Exception {
        this.publicKey = reader.readString();
    }

    @Override
    public void handle() throws Exception {
        final byte[] publicKeyBytes = Base64.getMimeDecoder().decode(StringUtility.rotate(-3, this.publicKey));
        final PublicKey serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        final KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        final SecretKey aesKey = keyGen.generateKey();

        // using RSA/ECB/OAEPPadding with a custom spec instead of
        // RSA/ECB/OAEPWithSHA-256AndMGF1Padding (SHA-1 MGF1) for compatibility reasons
        final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        final OAEPParameterSpec oaepSpec = new OAEPParameterSpec(
                "SHA-256", "MGF1",
                new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT
        );
        rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey, oaepSpec);

        final byte[] encryptedAESKeyBytes = Base64.getEncoder().encode(rsaCipher.doFinal(aesKey.getEncoded()));
        final String encryptedAESKey = new String(encryptedAESKeyBytes, StandardCharsets.UTF_8);

        ClientSocket.getInstance().setEncryptionContext(new EncryptionContext(aesKey));
        ClientSocket.getInstance().sendPacket(new C2SUpgradePacket(encryptedAESKey));
    }

    @Override
    public int id() {
        return 0;
    }

}
