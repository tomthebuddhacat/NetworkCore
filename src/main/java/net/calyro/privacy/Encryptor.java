package net.calyro.privacy;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import net.calyro.Config;

import java.util.Base64;

public class Encryptor {

    private final SecretKeySpec secretKey;
    String key = (String) Config.get("key");

    public Encryptor() {
        if (key.length() != 16) {
            throw new IllegalArgumentException("Encryption key must be 16 characters");
        }
        this.secretKey = new SecretKeySpec(key.getBytes(), "AES");
    }

    public String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(strToEncrypt.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(strToDecrypt);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
