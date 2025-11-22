package com.example.spending_management_app.utils;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utility class for password hashing and verification
 * Uses PBKDF2 with SHA-256 for strong password security
 */
public class PasswordUtils {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536; // High iteration count for security
    private static final int KEY_LENGTH = 256; // 256-bit key
    private static final int SALT_LENGTH = 32; // 256-bit salt

    private static final SecureRandom random = new SecureRandom();

    /**
     * Hash a password with a randomly generated salt
     * @param password The plain text password
     * @return The hashed password in format: iterations:salt:hash
     */
    public static String hashPassword(String password) {
        byte[] salt = generateSalt();
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return ITERATIONS + ":" + Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    /**
     * Verify a password against a stored hash
     * @param password The plain text password to check
     * @param storedHash The stored hash in format: iterations:salt:hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            return false;
        }

        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] hash = Base64.decode(parts[2], Base64.NO_WRAP);

        byte[] testHash = pbkdf2(password.toCharArray(), salt, iterations, hash.length * 8);
        return MessageDigest.isEqual(hash, testHash);
    }

    /**
     * Generate a random salt
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * PBKDF2 key derivation function
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error while hashing password", e);
        }
    }

    /**
     * Generate a random username for phone number registration
     * Format: userXXXXX where XXXXX is random number
     */
    public static String generateRandomUsername() {
        int randomNum = random.nextInt(90000) + 10000; // 10000-99999
        return "user" + randomNum;
    }

    /**
     * Extract name from email (part before @)
     * @param email The email address
     * @return The name part, capitalized
     */
    public static String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "User";
        }

        String name = email.substring(0, email.indexOf("@"));
        // Capitalize first letter
        if (name.length() > 0) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }
        return name;
    }

    /**
     * Generate default avatar URL based on name
     * Uses a simple hash to create consistent avatar colors
     */
    public static String generateDefaultAvatar(String name) {
        if (name == null || name.isEmpty()) {
            name = "User";
        }

        // Simple hash for consistent color generation
        int hash = name.hashCode();
        int colorIndex = Math.abs(hash) % 6; // 6 different colors

        String[] colors = {
            "FF6B6B", "4ECDC4", "45B7D1", "96CEB4", "FFEAA7", "DDA0DD"
        };

        String color = colors[colorIndex];
        String initial = name.substring(0, 1).toUpperCase();

        // Return a simple SVG avatar URL (you can replace with actual avatar service)
        return "https://ui-avatars.com/api/?name=" + initial + "&color=FFFFFF&background=" + color + "&size=128";
    }
}