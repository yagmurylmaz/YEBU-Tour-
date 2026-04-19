package org.mindrot.jbcrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class BCrypt {
    private static final SecureRandom RANDOM = new SecureRandom();

    private BCrypt() {}

    public static String gensalt(int logRounds) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashpw(String password, String salt) {
        return "sha256$" + salt + "$" + digest(password, salt);
    }

    public static boolean checkpw(String password, String hashed) {
        if (hashed == null || !hashed.startsWith("sha256$")) return false;
        String[] parts = hashed.split("\\$");
        if (parts.length != 3) return false;
        String expected = hashpw(password, parts[1]);
        return expected.equals(hashed);
    }

    private static String digest(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] out = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }
}
