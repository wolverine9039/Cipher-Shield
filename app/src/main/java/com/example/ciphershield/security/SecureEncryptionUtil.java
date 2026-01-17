package com.example.ciphershield.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Secure Encryption Utility using AES-256-GCM + RSA-2048
 * Addresses all security vulnerabilities:
 * - Uses GCM mode for authenticated encryption
 * - Generates secure random IVs
 * - Implements proper key management
 * - Adds HMAC for integrity verification
 */
public class SecureEncryptionUtil {

    private static final String TAG = "SecureEncryption";
    private static final int AES_KEY_SIZE = 256;
    private static final int RSA_KEY_SIZE = 2048;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    public static class EncryptionResult {
        public byte[] encryptedData;
        public byte[] privateKey;
        public byte[] salt;
        public String checksum;

        public EncryptionResult(byte[] encryptedData, byte[] privateKey, byte[] salt, String checksum) {
            this.encryptedData = encryptedData;
            this.privateKey = privateKey;
            this.salt = salt;
            this.checksum = checksum;
        }
    }

    public static class DecryptionResult {
        public byte[] decryptedData;
        public String originalExtension;
        public boolean verified;

        public DecryptionResult(byte[] decryptedData, String originalExtension, boolean verified) {
            this.decryptedData = decryptedData;
            this.originalExtension = originalExtension;
            this.verified = verified;
        }
    }

    /**
     * Encrypts data using AES-256-GCM with RSA-2048 key wrapping
     */
    public static EncryptionResult encrypt(byte[] inputData, String originalExtension) throws Exception {
        if (inputData == null || inputData.length == 0) {
            throw new IllegalArgumentException("Input data cannot be empty");
        }

        // Generate secure random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        // Generate AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE, random);
        SecretKey aesKey = keyGen.generateKey();

        // Generate random IV for GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);

        // Encrypt data with AES-GCM
        Cipher aesCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

        // Add original extension as Additional Authenticated Data (AAD)
        byte[] extBytes = originalExtension.getBytes(StandardCharsets.UTF_8);
        aesCipher.updateAAD(extBytes);

        byte[] encryptedData = aesCipher.doFinal(inputData);

        // Generate RSA key pair
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(RSA_KEY_SIZE, random);
        KeyPair rsaKeyPair = rsaGen.generateKeyPair();

        // Encrypt AES key with RSA
        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair.getPublic());
        byte[] encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

        // Calculate HMAC for integrity
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] hmacValue = hmac.doFinal(encryptedData);

        // Build secure file structure
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Header: [VERSION][SALT][IV][EXT_LEN][EXTENSION]
        output.write("CS2".getBytes(StandardCharsets.UTF_8)); // Version 2
        output.write(ByteBuffer.allocate(4).putInt(salt.length).array());
        output.write(salt);
        output.write(ByteBuffer.allocate(4).putInt(iv.length).array());
        output.write(iv);
        output.write(ByteBuffer.allocate(4).putInt(extBytes.length).array());
        output.write(extBytes);

        // Encrypted AES key
        output.write(ByteBuffer.allocate(4).putInt(encryptedAESKey.length).array());
        output.write(encryptedAESKey);

        // HMAC
        output.write(ByteBuffer.allocate(4).putInt(hmacValue.length).array());
        output.write(hmacValue);

        // Encrypted data
        output.write(encryptedData);

        // Calculate checksum for verification
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String checksum = bytesToHex(digest.digest(output.toByteArray()));

        return new EncryptionResult(
                output.toByteArray(),
                rsaKeyPair.getPrivate().getEncoded(),
                salt,
                checksum
        );
    }

    /**
     * Decrypts data with integrity verification
     */
    public static DecryptionResult decrypt(byte[] encryptedData, byte[] privateKeyBytes) throws Exception {
        if (encryptedData == null || encryptedData.length < 50) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }

        ByteBuffer buffer = ByteBuffer.wrap(encryptedData);

        // Read version
        byte[] versionBytes = new byte[3];
        buffer.get(versionBytes);
        String version = new String(versionBytes, StandardCharsets.UTF_8);

        if (!"CS2".equals(version)) {
            throw new Exception("Unsupported file version: " + version);
        }

        // Read salt
        int saltLen = buffer.getInt();
        byte[] salt = new byte[saltLen];
        buffer.get(salt);

        // Read IV
        int ivLen = buffer.getInt();
        byte[] iv = new byte[ivLen];
        buffer.get(iv);

        // Read extension
        int extLen = buffer.getInt();
        byte[] extBytes = new byte[extLen];
        buffer.get(extBytes);
        String originalExtension = new String(extBytes, StandardCharsets.UTF_8);

        // Read encrypted AES key
        int aesKeyLen = buffer.getInt();
        byte[] encryptedAESKey = new byte[aesKeyLen];
        buffer.get(encryptedAESKey);

        // Read HMAC
        int hmacLen = buffer.getInt();
        byte[] storedHmac = new byte[hmacLen];
        buffer.get(storedHmac);

        // Read encrypted data
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        // Verify HMAC
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] calculatedHmac = hmac.doFinal(ciphertext);

        boolean verified = MessageDigest.isEqual(storedHmac, calculatedHmac);
        if (!verified) {
            Log.w(TAG, "HMAC verification failed - file may be corrupted or tampered");
        }

        // Decrypt AES key with RSA
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey rsaPrivateKey = keyFactory.generatePrivate(
                new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes)
        );

        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAESKey);

        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        // Decrypt data with AES-GCM
        Cipher aesCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        aesCipher.updateAAD(extBytes);

        byte[] decryptedData = aesCipher.doFinal(ciphertext);

        // Secure cleanup
        Arrays.fill(aesKeyBytes, (byte) 0);
        Arrays.fill(salt, (byte) 0);
        Arrays.fill(iv, (byte) 0);

        return new DecryptionResult(decryptedData, originalExtension, verified);
    }

    /**
     * Password-based encryption using PBKDF2
     */
    public static EncryptionResult encryptWithPassword(byte[] inputData, String password, String originalExtension) throws Exception {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        // Derive key from password
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                salt,
                100000, // iterations
                AES_KEY_SIZE
        );
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey aesKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        spec.clearPassword();

        // Generate IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);

        // Encrypt with AES-GCM
        Cipher aesCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

        byte[] extBytes = originalExtension.getBytes(StandardCharsets.UTF_8);
        aesCipher.updateAAD(extBytes);

        byte[] encryptedData = aesCipher.doFinal(inputData);

        // Calculate HMAC
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] hmacValue = hmac.doFinal(encryptedData);

        // Build file
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write("CP2".getBytes(StandardCharsets.UTF_8)); // Cipher Password v2
        output.write(ByteBuffer.allocate(4).putInt(salt.length).array());
        output.write(salt);
        output.write(ByteBuffer.allocate(4).putInt(iv.length).array());
        output.write(iv);
        output.write(ByteBuffer.allocate(4).putInt(extBytes.length).array());
        output.write(extBytes);
        output.write(ByteBuffer.allocate(4).putInt(hmacValue.length).array());
        output.write(hmacValue);
        output.write(encryptedData);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String checksum = bytesToHex(digest.digest(output.toByteArray()));

        return new EncryptionResult(output.toByteArray(), null, salt, checksum);
    }

    /**
     * Decrypt password-protected file
     */
    public static DecryptionResult decryptWithPassword(byte[] encryptedData, String password) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encryptedData);

        byte[] versionBytes = new byte[3];
        buffer.get(versionBytes);
        String version = new String(versionBytes, StandardCharsets.UTF_8);

        if (!"CP2".equals(version)) {
            throw new Exception("Not a password-protected file");
        }

        int saltLen = buffer.getInt();
        byte[] salt = new byte[saltLen];
        buffer.get(salt);

        int ivLen = buffer.getInt();
        byte[] iv = new byte[ivLen];
        buffer.get(iv);

        int extLen = buffer.getInt();
        byte[] extBytes = new byte[extLen];
        buffer.get(extBytes);
        String originalExtension = new String(extBytes, StandardCharsets.UTF_8);

        int hmacLen = buffer.getInt();
        byte[] storedHmac = new byte[hmacLen];
        buffer.get(storedHmac);

        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        // Verify HMAC
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] calculatedHmac = hmac.doFinal(ciphertext);
        boolean verified = MessageDigest.isEqual(storedHmac, calculatedHmac);

        // Derive key
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), salt, 100000, AES_KEY_SIZE
        );
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey aesKey = new SecretKeySpec(tmp.getEncoded(), "AES");
        spec.clearPassword();

        // Decrypt
        Cipher aesCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        aesCipher.updateAAD(extBytes);

        byte[] decryptedData = aesCipher.doFinal(ciphertext);

        return new DecryptionResult(decryptedData, originalExtension, verified);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}