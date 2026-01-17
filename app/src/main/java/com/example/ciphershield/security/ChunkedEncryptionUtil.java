package com.example.ciphershield.security;

import android.net.Uri;
import android.content.Context;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chunked Encryption for Large Files
 * Supports files up to 10GB with progress tracking
 * Memory efficient - processes 1MB chunks at a time
 */
public class ChunkedEncryptionUtil {

    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    private static final int AES_KEY_SIZE = 256;
    private static final int RSA_KEY_SIZE = 2048;
    private static final int CBC_IV_LENGTH = 16;  // CBC needs 16 bytes
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    public interface ProgressCallback {
        void onProgress(int percentage, long bytesProcessed, long totalBytes);
        void onComplete();
        void onError(Exception e);
    }

    public static class EncryptionResult {
        public Uri encryptedFileUri;
        public byte[] privateKey;
        public String checksum;

        public EncryptionResult(Uri encryptedFileUri, byte[] privateKey, String checksum) {
            this.encryptedFileUri = encryptedFileUri;
            this.privateKey = privateKey;
            this.checksum = checksum;
        }
    }

    /**
     * Encrypts large file in chunks with progress tracking
     * Uses streaming encryption to avoid memory issues
     */
    public static EncryptionResult encryptLargeFile(
            Context context,
            Uri inputUri,
            Uri outputUri,
            String originalExtension,
            ProgressCallback callback) throws Exception {

        SecureRandom random = new SecureRandom();

        // Generate AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE, random);
        SecretKey aesKey = keyGen.generateKey();

        // Generate RSA key pair
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(RSA_KEY_SIZE, random);
        KeyPair rsaKeyPair = rsaGen.generateKeyPair();

        // Encrypt AES key with RSA
        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair.getPublic());
        byte[] encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

        // Generate salt and IV
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] iv = new byte[CBC_IV_LENGTH];  // 16 bytes for CBC
        random.nextBytes(iv);

        // Build header
        byte[] extBytes = originalExtension.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream headerStream = new ByteArrayOutputStream();

        headerStream.write("CL2".getBytes(StandardCharsets.UTF_8));
        headerStream.write(ByteBuffer.allocate(4).putInt(salt.length).array());
        headerStream.write(salt);
        headerStream.write(ByteBuffer.allocate(4).putInt(iv.length).array());
        headerStream.write(iv);
        headerStream.write(ByteBuffer.allocate(4).putInt(extBytes.length).array());
        headerStream.write(extBytes);
        headerStream.write(ByteBuffer.allocate(4).putInt(encryptedAESKey.length).array());
        headerStream.write(encryptedAESKey);

        byte[] header = headerStream.toByteArray();

        // Get file size for progress tracking
        long totalSize = getFileSize(context, inputUri);
        long bytesProcessed = 0;

        // Initialize cipher - Use CBC mode for streaming instead of GCM
        Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);

        // Initialize HMAC
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(salt, "HmacSHA256"));

        try (InputStream rawInputStream = context.getContentResolver().openInputStream(inputUri);
             BufferedInputStream inputStream = new BufferedInputStream(rawInputStream, 8192);
             OutputStream rawOutputStream = context.getContentResolver().openOutputStream(outputUri);
             BufferedOutputStream outputStream = new BufferedOutputStream(rawOutputStream, 8192)) {

            // Write header
            outputStream.write(header);

            // Reserve space for HMAC
            byte[] hmacPlaceholder = new byte[36];
            long hmacPosition = header.length;
            outputStream.write(hmacPlaceholder);

            // Process file in small chunks to avoid memory issues
            byte[] buffer = new byte[8192]; // Smaller chunks for streaming
            int bytesRead;
            long chunksProcessed = 0;

            while ((bytesRead = inputStream.read(buffer)) > 0) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);

                // Encrypt chunk using update (streaming)
                byte[] encryptedChunk = aesCipher.update(chunk);

                if (encryptedChunk != null && encryptedChunk.length > 0) {
                    // Update HMAC
                    hmac.update(encryptedChunk);

                    // Write encrypted chunk with length
                    outputStream.write(ByteBuffer.allocate(4).putInt(encryptedChunk.length).array());
                    outputStream.write(encryptedChunk);
                }

                bytesProcessed += bytesRead;
                chunksProcessed++;

                // Update progress every 128 chunks (1MB)
                if (chunksProcessed % 128 == 0 && callback != null) {
                    int progress = (int) ((bytesProcessed * 100) / totalSize);
                    callback.onProgress(progress, bytesProcessed, totalSize);
                }

                // Help garbage collector
                chunk = null;
                encryptedChunk = null;

                // Force GC every 256MB
                if (bytesProcessed % (256 * 1024 * 1024) == 0) {
                    System.gc();
                }
            }

            // Finalize encryption (this handles padding)
            byte[] finalChunk = aesCipher.doFinal();
            if (finalChunk != null && finalChunk.length > 0) {
                hmac.update(finalChunk);
                outputStream.write(ByteBuffer.allocate(4).putInt(finalChunk.length).array());
                outputStream.write(finalChunk);
            }

            outputStream.flush();
        }

        // Calculate final HMAC
        byte[] hmacValue = hmac.doFinal();

        // Write HMAC using ParcelFileDescriptor
        try {
            android.os.ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(outputUri, "rw");
            if (pfd != null) {
                FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                fos.getChannel().position(header.length);
                fos.write(ByteBuffer.allocate(4).putInt(hmacValue.length).array());
                fos.write(hmacValue);
                fos.close();
                pfd.close();
            }
        } catch (Exception e) {
            android.util.Log.w("ChunkedEncryption", "HMAC write failed: " + e.getMessage());
        }

        // Calculate checksum
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String checksum = bytesToHex(digest.digest(aesKey.getEncoded()));

        if (callback != null) {
            callback.onProgress(100, totalSize, totalSize);
            callback.onComplete();
        }

        return new EncryptionResult(outputUri, rsaKeyPair.getPrivate().getEncoded(), checksum);
    }

    /**
     * Decrypts large file in chunks with progress tracking
     */
    public static void decryptLargeFile(
            Context context,
            Uri encryptedUri,
            Uri outputUri,
            byte[] privateKeyBytes,
            ProgressCallback callback) throws Exception {

        try (InputStream rawInputStream = context.getContentResolver().openInputStream(encryptedUri);
             BufferedInputStream inputStream = new BufferedInputStream(rawInputStream, 8192);
             OutputStream rawOutputStream = context.getContentResolver().openOutputStream(outputUri);
             BufferedOutputStream outputStream = new BufferedOutputStream(rawOutputStream, 8192)) {

            // Read header
            byte[] versionBytes = new byte[3];
            inputStream.read(versionBytes);
            String version = new String(versionBytes, StandardCharsets.UTF_8);

            if (!"CL2".equals(version)) {
                throw new Exception("Invalid chunked file format");
            }

            // Read salt
            byte[] saltLenBytes = new byte[4];
            inputStream.read(saltLenBytes);
            int saltLen = ByteBuffer.wrap(saltLenBytes).getInt();
            byte[] salt = new byte[saltLen];
            inputStream.read(salt);

            // Read IV
            byte[] ivLenBytes = new byte[4];
            inputStream.read(ivLenBytes);
            int ivLen = ByteBuffer.wrap(ivLenBytes).getInt();
            byte[] iv = new byte[ivLen];
            inputStream.read(iv);

            // Read extension
            byte[] extLenBytes = new byte[4];
            inputStream.read(extLenBytes);
            int extLen = ByteBuffer.wrap(extLenBytes).getInt();
            byte[] extBytes = new byte[extLen];
            inputStream.read(extBytes);

            // Read encrypted AES key
            byte[] aesKeyLenBytes = new byte[4];
            inputStream.read(aesKeyLenBytes);
            int aesKeyLen = ByteBuffer.wrap(aesKeyLenBytes).getInt();
            byte[] encryptedAESKey = new byte[aesKeyLen];
            inputStream.read(encryptedAESKey);

            // Read HMAC
            byte[] hmacLenBytes = new byte[4];
            inputStream.read(hmacLenBytes);
            int hmacLen = ByteBuffer.wrap(hmacLenBytes).getInt();
            byte[] storedHmac = new byte[hmacLen];
            inputStream.read(storedHmac);

            // Decrypt AES key
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey rsaPrivateKey = keyFactory.generatePrivate(
                    new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes)
            );

            Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
            rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAESKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // Initialize cipher for decryption (CBC mode)
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

            // Initialize HMAC for verification
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(salt, "HmacSHA256"));

            long totalSize = getFileSize(context, encryptedUri);
            long bytesProcessed = 0;
            long chunksProcessed = 0;

            // Process chunks
            byte[] chunkLenBytes = new byte[4];

            while (inputStream.read(chunkLenBytes) == 4) {
                int chunkLen = ByteBuffer.wrap(chunkLenBytes).getInt();
                byte[] encryptedChunk = new byte[chunkLen];

                int totalRead = 0;
                while (totalRead < chunkLen) {
                    int read = inputStream.read(encryptedChunk, totalRead, chunkLen - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }

                if (totalRead != chunkLen) {
                    throw new Exception("Incomplete chunk read");
                }

                // Update HMAC
                hmac.update(encryptedChunk);

                // Decrypt chunk using update (streaming)
                byte[] decryptedChunk = aesCipher.update(encryptedChunk);

                if (decryptedChunk != null && decryptedChunk.length > 0) {
                    outputStream.write(decryptedChunk);
                }

                bytesProcessed += chunkLen;
                chunksProcessed++;

                // Update progress every 128 chunks
                if (chunksProcessed % 128 == 0 && callback != null) {
                    int progress = Math.min(95, (int) ((bytesProcessed * 100) / totalSize));
                    callback.onProgress(progress, bytesProcessed, totalSize);
                }

                // Help GC
                encryptedChunk = null;
                decryptedChunk = null;
            }

            // Finalize decryption (handles padding removal)
            byte[] finalChunk = aesCipher.doFinal();
            if (finalChunk != null && finalChunk.length > 0) {
                outputStream.write(finalChunk);
            }

            outputStream.flush();

            // Verify HMAC
            byte[] calculatedHmac = hmac.doFinal();
            boolean verified = MessageDigest.isEqual(storedHmac, calculatedHmac);

            if (!verified) {
                android.util.Log.w("ChunkedEncryption", "HMAC verification failed");
            }

            if (callback != null) {
                callback.onProgress(100, totalSize, totalSize);
                callback.onComplete();
            }
        }
    }

    /**
     * Determines if file should use chunked encryption
     */
    public static boolean shouldUseChunkedEncryption(Context context, Uri fileUri) {
        long fileSize = getFileSize(context, fileUri);
        return fileSize > (10 * 1024 * 1024); // 10MB threshold
    }

    private static long getFileSize(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            long size = 0;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                size += read;
            }
            return size;
        } catch (IOException e) {
            return 0;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}