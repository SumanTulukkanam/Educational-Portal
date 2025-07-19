// Add this new service class for DSC password encryption
package com.example.demo.service;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class DSCEncryptionService {

    // Use a fixed key for simplicity - in production, use a more secure key management
    private static final String SECRET_KEY = "MySecretKey12345"; // 16 characters for AES-128
    private static final String ALGORITHM = "AES";

    public String encryptDscPassword(String plainPassword) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(plainPassword.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting DSC password", e);
        }
    }

    public String decryptDscPassword(String encryptedPassword) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decryptedBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting DSC password", e);
        }
    }
}