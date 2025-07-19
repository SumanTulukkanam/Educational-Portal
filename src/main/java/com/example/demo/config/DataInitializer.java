package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.example.demo.model.Admin;
import com.example.demo.model.AuthConfig;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.AuthConfigRepository;
import com.example.demo.service.PasswordService;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AuthConfigRepository authConfigRepository;

    @Autowired
    private PasswordService passwordService;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin if not exists
        if (!adminRepository.existsByUsername("admin")) {
            Admin defaultAdmin = new Admin();
            defaultAdmin.setUsername("admin");
            defaultAdmin.setPassword(passwordService.encodePassword("admin123"));
            defaultAdmin.setFullName("System Administrator");
            defaultAdmin.setActive(true);
            adminRepository.save(defaultAdmin);
            System.out.println("Default admin created: username=admin, password=admin123");
        }

        // Create default auth config if not exists
        if (authConfigRepository.findActiveAuthConfig().isEmpty()) {
            AuthConfig defaultAuthConfig = new AuthConfig();
            defaultAuthConfig.setAuthMethod(AuthConfig.AuthMethod.PASSWORD_ONLY);
            defaultAuthConfig.setActive(true);
            defaultAuthConfig.setDescription("Default password-only authentication");
            authConfigRepository.save(defaultAuthConfig);
            System.out.println("Default auth config created: PASSWORD_ONLY");
        }
    }
}