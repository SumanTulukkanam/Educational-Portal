package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOTPEmail(String toEmail, String otp, String studentName) {
        try {
            System.out.println("=== EMAIL DEBUG INFO ===");
            System.out.println("From email: " + fromEmail);
            System.out.println("To email: " + toEmail);
            System.out.println("OTP: " + otp);
            System.out.println("Student name: " + studentName);
            System.out.println("========================");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Email Verification - Student Portal");
            message.setText("Hello " + studentName + ",\n\n" +
                "Thank you for registering with Student Portal!\n\n" +
                "Your OTP for email verification is: " + otp + "\n\n" +
                "This OTP will expire in 10 minutes.\n" +
                "Please enter this OTP to complete your registration.\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Best regards,\nStudent Portal Team");

            mailSender.send(message);
            System.out.println("✓ OTP email sent successfully to: " + toEmail);

        } catch (org.springframework.mail.MailAuthenticationException e) {
            System.err.println("❌ MAIL AUTHENTICATION FAILED!");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Check your Gmail App Password in application.properties");
            throw new RuntimeException("Email authentication failed. Please check Gmail app password configuration.");

        } catch (org.springframework.mail.MailSendException e) {
            System.err.println("❌ MAIL SEND EXCEPTION!");
            System.err.println("Error: " + e.getMessage());
            if (e.getMessage().contains("Invalid Addresses")) {
                throw new RuntimeException("Invalid email address format.");
            }
            throw new RuntimeException("Failed to send email. Please check network connection and email configuration.");

        } catch (Exception e) {
            System.err.println("❌ UNEXPECTED EMAIL ERROR!");
            System.err.println("Error type: " + e.getClass().getSimpleName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    public void sendResetPasswordEmail(String toEmail, String newPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset - Student Portal");
            message.setText("Hello,\n\nYour password has been reset successfully.\n" +
                "Your new password is: " + newPassword +
                "\n\nPlease login and change your password.\n\n" +
                "Best regards,\nStudent Portal Team");

            mailSender.send(message);
            System.out.println("✓ Password reset email sent successfully to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
        }
    }

    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public boolean testEmailConnection() {
        try {
            System.out.println("🔄 Testing email configuration...");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(fromEmail);
            message.setSubject("Test Email - Student Portal");
            message.setText("This is a test email to verify email configuration.\n" +
                "If you receive this, your email setup is working correctly!");

            mailSender.send(message);
            System.out.println("✓ Test email sent successfully!");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Test email failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}