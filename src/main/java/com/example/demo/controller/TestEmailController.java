package com.example.demo.controller;

import com.example.demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestEmailController {
    
    @Autowired
    private EmailService emailService;
    
    @GetMapping("/test-email")
    public String testEmail() {
        try {
            boolean success = emailService.testEmailConnection();
            if (success) {
                return "✅ Email configuration is working! Check your email inbox.";
            } else {
                return "Email configuration failed. Check console logs for details.";
            }
        } catch (Exception e) {
            return "❌ Email test failed: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-otp")
    public String testOTP() {
        try {
            String otp = emailService.generateOTP();
            emailService.sendOTPEmail("testmail.project2004@gmail.com", otp, "Test User");
            return "✅ OTP email sent successfully! OTP: " + otp;
        } catch (Exception e) {
            return "❌ OTP email failed: " + e.getMessage();
        }
    }
}