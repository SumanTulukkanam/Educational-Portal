package com.example.demo.model;
import jakarta.persistence.*;

@Entity
@Table(name = "auth_config")
public class AuthConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false)
    private AuthMethod authMethod;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "description")
    private String description;
    
    public enum AuthMethod {
        PASSWORD_ONLY("Password Only"),
        OTP("OTP Verification"),
        DSC("Digital Signature Certificate"),
        PASSWORD_AND_OTP("Password + OTP"),
        PASSWORD_AND_DSC("Password + DSC");
        
        private String displayName;
        
        AuthMethod(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Constructors
    public AuthConfig() {}
    
    public AuthConfig(AuthMethod authMethod, boolean isActive) {
        this.authMethod = authMethod;
        this.isActive = isActive;
    }
    
    public AuthConfig(AuthMethod authMethod, boolean isActive, String description) {
        this.authMethod = authMethod;
        this.isActive = isActive;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public AuthMethod getAuthMethod() {
        return authMethod;
    }
    
    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
