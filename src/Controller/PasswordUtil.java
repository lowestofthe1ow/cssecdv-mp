package Controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    /* Java CSPRNG, best practice for secure random number generation. */
    private static final SecureRandom RNG = new SecureRandom();

    private static final String DUMMY_SALT = Base64.getEncoder()
            .encodeToString("dummy-salt-0123456".getBytes(StandardCharsets.UTF_8));

    private PasswordUtil() {}

    /** Generates a 16-byte random salt encoded as Base64. */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Derives a Base64-encoded PBKDF2-HMAC-SHA256 hash and clears the key spec afterward.
     * @param password plaintext password characters
     * @param saltB64 Base64-encoded salt bytes
     */
    public static String hash(char[] password, String saltB64) {
        PBEKeySpec spec = new PBEKeySpec(password, Base64.getDecoder().decode(saltB64), ITERATIONS, KEY_BITS);
        try {
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException("hashing failed");
        } finally {
            spec.clearPassword();
        }
    }

    
    /**
     * Verifies a password against a stored Base64 salt and hash in constant time.
     * @param password candidate password characters
     * @param saltB64 stored Base64 salt
     * @param storedHashB64 stored Base64 PBKDF2 hash
     */
    public static boolean verify(char[] password, String saltB64, String storedHashB64) {
        if (saltB64 == null || storedHashB64 == null) {
            return false;
        }
        byte[] actual = hash(password, saltB64).getBytes(StandardCharsets.UTF_8);
        byte[] expected = storedHashB64.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }

    /** Performs a dummy PBKDF2 hash to equalize timing on invalid or unknown-user logins. */
    public static void dummyVerify() {
        try {
            hash("dummy-password".toCharArray(), DUMMY_SALT);
        } catch (Exception ignored) {
        }
    }
}
