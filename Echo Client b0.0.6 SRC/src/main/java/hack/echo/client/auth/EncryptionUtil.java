package hack.echo.client.auth;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

// "+auth-related"
public class EncryptionUtil {

    private static final String PUBLIC_KEY_PEM = 
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApSEhYrwvpfzcEQ9WPnCw" +
        "ukLWlW7nOZpHv63h2UnYULAf+SUE3mt/sf2BdzWjobeWHZALt5eur1iXYe0BVK8j" +
        "ynRx4OUSePXj0nLJWialZKBCU4tIOmCD4P/NIzXs/SVy8emNXWi9ttKyDAp5agXU" +
        "PBC30JWsJwwkWYmYJtK2DxICTAr82Cjp5WdiG/zVZbD8F9dgUscEITXLQKyTKtak" +
        "HWbIWI0telONFu6+znuPJj4yM+Xe8O3xyilNWfPo5BGe9MGk9yseA/HqQvW61L6+" +
        "1P3Oxva6L1gMv+vU6rgozzSHyTb3Sf1GodJjeZxfFE4JSh1kA3jIs5v2nq7QMWcU" +
        "8QIDAQAB";

    private static PublicKey serverPublicKey;
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    static {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(PUBLIC_KEY_PEM);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            serverPublicKey = kf.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load server public key", e);
        }
    }

    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    public static String encryptRSA(SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
        byte[] encryptedBytes = cipher.doFinal(aesKey.getEncoded());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static EncryptedPacket encryptAES(String json, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new java.security.SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));

        String ivBase64 = Base64.getEncoder().encodeToString(iv);
        String dataBase64 = Base64.getEncoder().encodeToString(encrypted);

        return new EncryptedPacket(ivBase64, dataBase64);
    }

    public static String decryptAES(String ivBase64, String dataBase64, SecretKey key) throws Exception {
        byte[] iv = Base64.getDecoder().decode(ivBase64);
        byte[] encrypted = Base64.getDecoder().decode(dataBase64);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static class EncryptedPacket {
        public final String iv;
        public final String d;

        public EncryptedPacket(String iv, String d) {
            this.iv = iv;
            this.d = d;
        }
    }
}
