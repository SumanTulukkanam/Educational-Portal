package com.example.demo.service;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Base64;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Service
public class DSCService {

    private final String DSC_STORAGE_PATH = "dsc-certificates/";
    private final String TEMP_DSC_PATH = "uploads/temp_dsc/";
    private final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    
    public DSCService() {
        try {
            Files.createDirectories(Paths.get(DSC_STORAGE_PATH));
            Files.createDirectories(Paths.get(TEMP_DSC_PATH));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Validates DSC file and password
     */
    public boolean validateDSC(String filePath, String password) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.canRead()) {
                System.err.println("DSC file does not exist or cannot be read: " + filePath);
                return false;
            }
            
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(filePath)) {
                keyStore.load(fis, password != null ? password.toCharArray() : null);
            }
            
            // Check if keystore has at least one valid certificate with private key
            Enumeration<String> aliases = keyStore.aliases();
            boolean hasValidEntry = false;
            
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    Certificate cert = keyStore.getCertificate(alias);
                    PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());

                    if (cert instanceof X509Certificate && privateKey != null) {
                        X509Certificate x509 = (X509Certificate) cert;
                        try {
                            x509.checkValidity(); // Check if certificate is valid (not expired)
                            hasValidEntry = true;
                            break;
                        } catch (Exception e) {
                            System.out.println("Certificate " + alias + " is expired or invalid");
                        }
                    }
                }
            }
            
            if (!hasValidEntry) {
                System.err.println("No valid certificate with private key found in keystore");
                return false;
            }
            
            System.out.println("DSC validation successful for: " + filePath);
            return true;
            
        } catch (Exception e) {
            System.err.println("DSC validation failed: " + e.getMessage());
            return false;
        }
    }

    public String generateChallenge(String studentId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String nonce = String.valueOf(System.nanoTime());
        return String.format("CHALLENGE:%s:%s:%s", studentId, timestamp, nonce);
    }
    
    /**
     * Signs a challenge message using the private key from DSC
     */
    public String signChallenge(String dscPath, String password, String challenge) throws Exception {
        KeyStore keyStore = loadKeyStore(dscPath, password);
        if (keyStore == null) {
            throw new Exception("Failed to load DSC keystore");
        }
        
        // Get the first valid private key entry
        PrivateKey privateKey = null;
        String alias = null;

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) cert;
                    try {
                        x509.checkValidity();
                        privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
                        if (privateKey != null) {
                            break;
                        }
                    } catch (Exception e) {
                        // Continue searching
                    }
                }
            }
        }
        
        if (privateKey == null) {
            throw new Exception("No valid private key found in DSC");
        }
        
        // Create digital signature
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(challenge.getBytes("UTF-8"));
        byte[] signatureBytes = signature.sign();
        
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
    
    /**
     * Verifies a signed challenge using the public key from registered DSC
     */
    public boolean verifySignature(String registeredDscPath, String registeredPassword,
                                 String challenge, String signatureString) throws Exception {
        KeyStore keyStore = loadKeyStore(registeredDscPath, registeredPassword);
        if (keyStore == null) {
            return false;
        }
        
        // Get the public key from registered certificate
        PublicKey publicKey = null;

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = keyStore.getCertificate(alias);
            if (cert instanceof X509Certificate) {
                X509Certificate x509 = (X509Certificate) cert;
                try {
                    x509.checkValidity();
                    publicKey = x509.getPublicKey();
                    break;
                } catch (Exception e) {
                    // Skip invalid certificates
                }
            }
        }
        
        if (publicKey == null) {
            return false;
        }
        
        try {
            // Verify the signature
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(challenge.getBytes("UTF-8"));
            byte[] signatureBytes = Base64.getDecoder().decode(signatureString);

            return signature.verify(signatureBytes);
        } catch (Exception e) {
            System.err.println("Signature verification failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Complete DSC authentication process:
     * 1. Generate challenge
     * 2. Sign challenge with uploaded DSC
     * 3. Verify signature with registered DSC
     */
    public DSCAuthResult authenticateWithDSC(String registeredDscPath, String registeredPassword,
                                           String uploadedDscPath, String uploadedPassword,
                                           String studentId) {
        try {
            // Step 1: Generate challenge
            String challenge = generateChallenge(studentId);

            // Step 2: Sign challenge with uploaded DSC
            String signature = signChallenge(uploadedDscPath, uploadedPassword, challenge);

            // Step 3: Verify signature with registered DSC
            boolean verified = verifySignature(registeredDscPath, registeredPassword, challenge, signature);

            return new DSCAuthResult(verified, challenge, signature,
                    verified ? "Authentication successful" : "Signature verification failed");

        } catch (Exception e) {
            return new DSCAuthResult(false, null, null, "Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Enhanced DSC comparison using certificate matching AND signature verification
     */
    public boolean authenticateDSCAdvanced(String registeredDscPath, String uploadedDscPath,
                                         String registeredPassword, String uploadedPassword,
                                         String studentId) {
        try {
            // First, do basic certificate comparison
            if (!compareDSCCertificates(registeredDscPath, uploadedDscPath,
                    registeredPassword, uploadedPassword)) {
                System.err.println("Certificate comparison failed");
                return false;
            }
            
            // Second, perform signature-based authentication
            DSCAuthResult authResult = authenticateWithDSC(registeredDscPath, registeredPassword,
                    uploadedDscPath, uploadedPassword, studentId);

            if (!authResult.isSuccess()) {
                System.err.println("Signature authentication failed: " + authResult.getMessage());
                return false;
            }
            
            System.out.println("Advanced DSC authentication successful");
            return true;
            
        } catch (Exception e) {
            System.err.println("Advanced DSC authentication error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Compare DSC certificates (original method, renamed for clarity)
     */
    public boolean compareDSCCertificates(String registeredDscPath, String uploadedDscPath,
                                        String registeredPassword, String uploadedPassword) {
        try {
            KeyStore registeredKeyStore = loadKeyStore(registeredDscPath, registeredPassword);
            KeyStore uploadedKeyStore = loadKeyStore(uploadedDscPath, uploadedPassword);
            
            if (registeredKeyStore == null || uploadedKeyStore == null) {
                return false;
            }
            
            X509Certificate registeredCert = getFirstValidCertificate(registeredKeyStore);
            X509Certificate uploadedCert = getFirstValidCertificate(uploadedKeyStore);
            
            if (registeredCert == null || uploadedCert == null) {
                System.err.println("Could not extract certificates for comparison");
                return false;
            }
            
            boolean matches = Arrays.equals(registeredCert.getEncoded(), uploadedCert.getEncoded());
            System.out.println("Certificate comparison result: " + (matches ? "MATCH" : "NO MATCH"));
            return matches;
            
        } catch (Exception e) {
            System.err.println("Error comparing DSC certificates: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Legacy method name for backward compatibility
     */
    public boolean compareDSCFiles(String registeredDscPath, String uploadedDscPath,
                                 String registeredPassword, String uploadedPassword) {
        return compareDSCCertificates(registeredDscPath, uploadedDscPath,
                registeredPassword, uploadedPassword);
    }
    
    private KeyStore loadKeyStore(String filePath, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(filePath)) {
                keyStore.load(fis, password != null ? password.toCharArray() : null);
            }
            return keyStore;
        } catch (Exception e) {
            System.err.println("Failed to load keystore: " + filePath + " - " + e.getMessage());
            return null;
        }
    }
    
    private X509Certificate getFirstValidCertificate(KeyStore keyStore) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) cert;
                    try {
                        x509.checkValidity();
                        return x509;
                    } catch (Exception e) {
                        // Skip expired certificates
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting certificate: " + e.getMessage());
        }
        return null;
    }
    
    public String saveDSCFile(MultipartFile file, String studentId) throws Exception {
        validateJksFile(file);
        
        String fileName = studentId + "_" + System.currentTimeMillis() + ".jks";
        Path filePath = Paths.get(DSC_STORAGE_PATH + fileName);
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }
    
    public String saveTempDSCFile(MultipartFile file, String studentId) throws Exception {
        validateJksFile(file);
        
        Path uploadPath = Paths.get(TEMP_DSC_PATH);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String fileName = studentId + "_temp_" + System.currentTimeMillis() + ".jks";
        Path filePath = uploadPath.resolve(fileName);
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }
    
    private void validateJksFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("DSC file is empty");
        }
        
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".jks")) {
            throw new Exception("Only .jks files are allowed for DSC certificates");
        }
    }
    
    public boolean deleteDSCFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Result class for DSC authentication
     */
    public static class DSCAuthResult {
        private final boolean success;
        private final String challenge;
        private final String signature;
        private final String message;
        
        public DSCAuthResult(boolean success, String challenge, String signature, String message) {
            this.success = success;
            this.challenge = challenge;
            this.signature = signature;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getChallenge() { return challenge; }
        public String getSignature() { return signature; }
        public String getMessage() { return message; }
    }
}