package com.example.demo.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
public class Student {

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamAttempt> examAttempts = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;
    
    @Column(name = "otp_code")
    private String otp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "is_verified")
    private boolean verified = false;

    // DSC (Digital Signature Certificate) FIELDS
    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "organization_unit")
    private String organizationUnit;

    @Column(name = "certificate_path")
    private String certificatePath;

    @Column(name = "certificate_alias")
    private String certificateAlias;

    @Column(name = "certificate_created_date")
    private LocalDateTime certificateCreatedDate;

    @Column(name = "dsc_password")
    private String dscPassword;
    
    @Column(name = "has_dsc")
    private boolean hasDsc = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_auth_method")
    private AuthConfig.AuthMethod preferredAuthMethod;
    
    // Using Boolean wrapper class to handle null values properly
    @Column(name = "auth_method_set_by_admin")
    private Boolean authMethodSetByAdmin = false;

    // CONSTRUCTORS
    public Student() {}

    public Student(String studentId, String name, String email, String password) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.verified = false;
        this.authMethodSetByAdmin = false; // Initialize with default value
    }

    // BASIC GETTERS AND SETTERS
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }

    public String getStudentId() { 
        return studentId; 
    }
    
    public void setStudentId(String studentId) { 
        this.studentId = studentId; 
    }

    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }

    public String getEmail() { 
        return email; 
    }
    
    public void setEmail(String email) { 
        this.email = email; 
    }

    public String getPassword() { 
        return password; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }

    public String getOtp() { 
        return otp; 
    }
    
    public void setOtp(String otp) { 
        this.otp = otp; 
    }

    public LocalDateTime getOtpExpiry() { 
        return otpExpiry; 
    }
    
    public void setOtpExpiry(LocalDateTime otpExpiry) { 
        this.otpExpiry = otpExpiry; 
    }

    public boolean isVerified() { 
        return verified; 
    }
    
    public void setVerified(boolean verified) { 
        this.verified = verified; 
    }

    public List<ExamAttempt> getExamAttempts() {
        return examAttempts;
    }

    public void setExamAttempts(List<ExamAttempt> examAttempts) {
        this.examAttempts = examAttempts;
    }

    // AUTHENTICATION METHOD GETTERS AND SETTERS
    public AuthConfig.AuthMethod getPreferredAuthMethod() {
        return preferredAuthMethod;
    }
    
    public void setPreferredAuthMethod(AuthConfig.AuthMethod preferredAuthMethod) {
        this.preferredAuthMethod = preferredAuthMethod;
    }
    
    public Boolean getAuthMethodSetByAdmin() {
        return authMethodSetByAdmin;
    }
    
    public void setAuthMethodSetByAdmin(Boolean authMethodSetByAdmin) {
        this.authMethodSetByAdmin = authMethodSetByAdmin;
    }

    // Helper method to safely check boolean value
    public boolean isAuthMethodSetByAdmin() {
        return authMethodSetByAdmin != null && authMethodSetByAdmin;
    }

    // DSC GETTERS AND SETTERS
    public String getOrganizationName() { 
        return organizationName; 
    }
    
    public void setOrganizationName(String organizationName) { 
        this.organizationName = organizationName; 
    }

    public String getOrganizationUnit() { 
        return organizationUnit; 
    }
    
    public void setOrganizationUnit(String organizationUnit) { 
        this.organizationUnit = organizationUnit; 
    }

    public String getCertificatePath() { 
        return certificatePath; 
    }
    
    public void setCertificatePath(String certificatePath) { 
        this.certificatePath = certificatePath; 
    }

    public String getCertificateAlias() { 
        return certificateAlias; 
    }
    
    public void setCertificateAlias(String certificateAlias) { 
        this.certificateAlias = certificateAlias; 
    }

    public LocalDateTime getCertificateCreatedDate() { 
        return certificateCreatedDate; 
    }
    
    public void setCertificateCreatedDate(LocalDateTime certificateCreatedDate) {
        this.certificateCreatedDate = certificateCreatedDate; 
    }

    public String getDscPassword() {
        return dscPassword;
    }
    
    public void setDscPassword(String dscPassword) {
        this.dscPassword = dscPassword;
    }
    
    public boolean isHasDsc() {
        return hasDsc;
    }
    
    public void setHasDsc(boolean hasDsc) {
        this.hasDsc = hasDsc;
    }
}