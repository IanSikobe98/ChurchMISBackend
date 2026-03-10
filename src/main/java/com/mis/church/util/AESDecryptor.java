package com.mis.church.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESDecryptor {

    private static final String SECRET_KEY = "3$RcX@8eWp9Tq3Ls"; // Must match the JS secret key

    public static String decrypt(String encryptedPassword) throws Exception {
        // Decode the base64-encoded string
        byte[] decodedKey = SECRET_KEY.getBytes("UTF-8");
        SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");

        // Initialize the cipher for decryption
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decrypt the password
        byte[] decodedPassword = Base64.decodeBase64(encryptedPassword);
        byte[] originalPassword = cipher.doFinal(decodedPassword);
        return new String(originalPassword);
    }
}
