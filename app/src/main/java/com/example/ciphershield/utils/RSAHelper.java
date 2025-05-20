
package com.example.ciphershield.utils;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RSAHelper {
    public static class KeyPair {
        public BigInteger e, d, n;

        public KeyPair(BigInteger e, BigInteger d, BigInteger n) {
            this.e = e;
            this.d = d;
            this.n = n;
        }
    }

    public static KeyPair generateKeys(int bitLength) {
        SecureRandom random = new SecureRandom();
        BigInteger p = BigInteger.probablePrime(bitLength, random);
        BigInteger q = BigInteger.probablePrime(bitLength, random);
        BigInteger n = p.multiply(q);
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger e = BigInteger.valueOf(65537);
        BigInteger d = e.modInverse(phi);
        return new KeyPair(e, d, n);
    }

    public static BigInteger encrypt(String plainText, BigInteger e, BigInteger n) {
        return new BigInteger(plainText.getBytes()).modPow(e, n);
    }

    public static String decrypt(BigInteger cipher, BigInteger d, BigInteger n) {
        return new String(cipher.modPow(d, n).toByteArray());
    }
}
