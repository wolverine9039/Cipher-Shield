package com.example.ciphershield;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class HybridEncryptionUtil {

    public static class Result {
        public byte[] encryptedData;
        public byte[] privateKey;

        public Result(byte[] encryptedData, byte[] privateKey) {
            this.encryptedData = encryptedData;
            this.privateKey = privateKey;
        }
    }

    public static Result encrypt(byte[] inputBytes, String originalExtension) throws Exception {
        // Generate AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();

        // Encrypt file data with AES
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encryptedFileData = aesCipher.doFinal(inputBytes);

        // Generate RSA key pair
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(2048);
        KeyPair rsaKeyPair = rsaGen.generateKeyPair();
        PrivateKey rsaPrivateKey = rsaKeyPair.getPrivate();

        // Encrypt AES key with RSA public key
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair.getPublic());
        byte[] encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

        // Build header: [HYB][extLen][ext][AES keyLen][AES key][encrypted data]
        byte[] methodBytes = "HYB".getBytes(StandardCharsets.UTF_8);
        byte[] extBytes = originalExtension.getBytes(StandardCharsets.UTF_8);
        byte[] extLen = ByteBuffer.allocate(4).putInt(extBytes.length).array();
        byte[] aesKeyLen = ByteBuffer.allocate(4).putInt(encryptedAESKey.length).array();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(methodBytes);  // "HYB"
        output.write(extLen);       // Extension length
        output.write(extBytes);     // Original extension (e.g., ".pdf")
        output.write(aesKeyLen);    // AES key length
        output.write(encryptedAESKey);
        output.write(encryptedFileData);

        return new Result(output.toByteArray(), rsaPrivateKey.getEncoded());
    }

    public static byte[] decryptHybrid(byte[] encryptedData, byte[] privateKeyBytes) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
        String method = new String(encryptedData, 0, 3, StandardCharsets.UTF_8);
        if (!"HYB".equals(method)) throw new Exception("Invalid hybrid encrypted data");

        int extLen = buffer.getInt(3);
        String originalExtension = new String(encryptedData, 7, extLen, StandardCharsets.UTF_8);
        int aesKeyLen = buffer.getInt(7 + extLen);
        int aesKeyStart = 11 + extLen;

        // Decrypt AES key with RSA private key
        PrivateKey rsaPrivateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] aesKeyBytes = rsaCipher.doFinal(
                Arrays.copyOfRange(encryptedData, aesKeyStart, aesKeyStart + aesKeyLen)
        );
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        // Decrypt file data
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
        return aesCipher.doFinal(
                Arrays.copyOfRange(encryptedData, aesKeyStart + aesKeyLen, encryptedData.length)
        );
    }
}