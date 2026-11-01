package com.example.bloggingplatform.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hashes passwords with salted PBKDF2-HMAC-SHA256 (built into the JDK).
 * <p>
 * Stored format: {@code pbkdf2-sha256$<iterations>$<salt>$<hash>} with Base64 salt and hash,
 * about 90 characters, which fits {@code users.password_hash VARCHAR(255)}. Because the
 * iteration count and salt are stored with the hash, the work factor can be raised later
 * without breaking existing accounts.
 */
public final class PasswordHasher {

    /** OWASP Password Storage Cheat Sheet recommendation for PBKDF2-HMAC-SHA256. */
    public static final int DEFAULT_ITERATIONS = 600_000;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    public PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    /** Lower iteration counts are only meant for fast unit tests. */
    public PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.iterations = iterations;
    }

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] key = derive(password, salt, iterations);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIX + "$" + iterations + "$" + base64.encodeToString(salt) + "$" + base64.encodeToString(key);
    }

    /** Checks a password against a stored hash in constant time. */
    public boolean matches(String password, String storedHash) {
        String[] parts = storedHash == null ? new String[0] : storedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int storedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, storedIterations));
        } catch (IllegalArgumentException e) {
            return false;   // malformed number or Base64
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        } finally {
            spec.clearPassword();
        }
    }
}
