package wtf.opal.utility.socket;

import wtf.opal.protection.annotation.NativeInclude;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@NativeInclude
public final class CipherUtility {

    private CipherUtility() {
    }

    public static byte[] generateIV() {
        final byte[] iv = new byte[12];
        final SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    public static String aesEncrypt(final String plainText, final SecretKey aesKey, final byte[] iv) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));

        final byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        final byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String aesDecrypt(final String encryptedText, final SecretKey aesKey) throws Exception {
        final byte[] decoded = Base64.getDecoder().decode(encryptedText);

        final byte[] iv = new byte[12];
        System.arraycopy(decoded, 0, iv, 0, 12);

        final byte[] cipherText = new byte[decoded.length - 12];
        System.arraycopy(decoded, 12, cipherText, 0, decoded.length - 12);

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));

        final byte[] decryptedBytes = cipher.doFinal(cipherText);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static String encryptWithPassphrase(final String plainText, final char[] passphrase) throws Exception {
        final byte[] salt = new byte[16];
        final SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        final int iterations = 10000;
        final int keyLength = 256;
        final byte[] iv = generateIV();

        final PBEKeySpec spec = new PBEKeySpec(passphrase, salt, iterations, keyLength);
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        final byte[] keyBytes = keyFactory.generateSecret(spec).getEncoded();
        final SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(salt);
        outputStream.write(iv);

        final byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        outputStream.write(cipherText);

        final byte[] combined = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decryptWithPassphrase(final String encryptedText, final char[] passphrase) throws Exception {
        final byte[] combined = Base64.getDecoder().decode(encryptedText);

        final byte[] salt = Arrays.copyOfRange(combined, 0, 16);
        final byte[] iv = Arrays.copyOfRange(combined, 16, 16 + 12);
        final byte[] cipherText = Arrays.copyOfRange(combined, 16 + 12, combined.length);

        final int iterations = 10000;
        final int keyLength = 256;

        final PBEKeySpec spec = new PBEKeySpec(passphrase, salt, iterations, keyLength);
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        final byte[] keyBytes = keyFactory.generateSecret(spec).getEncoded();
        final SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

        final byte[] decryptedBytes = cipher.doFinal(cipherText);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

}
