package com.example.demo.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class PasswordService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // For user login passwords (BCrypt - one way hashing)
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // For DSC passwords (Base64 - reversible encoding)
    public String encodeDscPassword(String rawPassword) {
        return Base64.getEncoder().encodeToString(rawPassword.getBytes());
    }

    public String decodePassword(String encodedPassword) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedPassword);
            return new String(decodedBytes);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid encoded DSC password format", e);
        }
    }
}