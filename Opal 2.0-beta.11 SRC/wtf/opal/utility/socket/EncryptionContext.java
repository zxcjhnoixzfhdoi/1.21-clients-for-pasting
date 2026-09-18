package wtf.opal.utility.socket;

import javax.crypto.SecretKey;

public record EncryptionContext(SecretKey aesKey) {
}
