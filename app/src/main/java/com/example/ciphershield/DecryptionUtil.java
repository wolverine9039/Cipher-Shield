package com.example.ciphershield;

import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class DecryptionUtil {

    public static class Result {
        public byte[] decryptedBytes;
        public String originalExtension;

        public Result(byte[] decryptedBytes, String originalExtension) {
            this.decryptedBytes = decryptedBytes;
            this.originalExtension = originalExtension;
        }
    }

    public static Result decrypt(byte[] encryptedData, byte[] keyBytes) throws Exception {
        if (encryptedData.length < 7) {
            throw new Exception("Invalid encrypted data");
        }

        String method = new String(encryptedData, 0, 3, StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
        int extLen = buffer.getInt(3);
        String originalExtension = new String(encryptedData, 7, extLen, StandardCharsets.UTF_8);

        byte[] data = Arrays.copyOfRange(encryptedData, 7 + extLen, encryptedData.length);

        if ("HYB".equals(method)) {
            return new Result(
                    HybridEncryptionUtil.decryptHybrid(encryptedData, keyBytes),
                    originalExtension
            );
        } else if ("HUF".equals(method)) {
            return new Result(
                    HuffmanEncryptionUtil.decompress(encryptedData, keyBytes),
                    originalExtension
            );
        } else {
            throw new Exception("Unsupported encryption method");
        }
    }
}