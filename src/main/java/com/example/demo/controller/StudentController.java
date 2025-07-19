package com.example.demo.controller;

import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.service.DSCService;
import com.example.demo.model.AuthConfig;
import com.example.demo.repository.AuthConfigRepository;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashMap;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.example.demo.model.Answer;
import com.example.demo.model.AttemptStatus;
import com.example.demo.model.Exam;
import com.example.demo.model.ExamAttempt;
import com.example.demo.model.Question;
import com.example.demo.model.Student;
import com.example.demo.repository.StudentRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.ExamService;
import com.example.demo.service.*;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private EmailService emailService;
    @Autowired private ExamService examService;
    @Autowired private PasswordService passwordService;
    @Autowired private DSCService dscService;
    @Autowired private AuthConfigRepository authConfigRepository;
    @Autowired private DSCEncryptionService dscEncryptionService;

    public class ExamWithAttemptInfo {
        private Exam exam;
        private ExamAttempt attempt;
        private String statusClass;

        public ExamWithAttemptInfo(Exam exam, ExamAttempt attempt) {
            this.exam = exam;
            this.attempt = attempt;
            this.statusClass = getStatusClass(attempt.getStatus());
        }

        private String getStatusClass(AttemptStatus status) {
            switch (status) {
                case COMPLETED:
                    return "status-completed";
                case TERMINATED:
                    return "status-terminated";
                case TIMED_OUT:
                    return "status-timed-out";
                default:
                    return "";
            }
        }

        // Getters
        public Exam getExam() { return exam; }
        public ExamAttempt getAttempt() { return attempt; }
        public String getStatusClass() { return statusClass; }
    }

    // Then in your controller:
    @GetMapping("/exams")
    public String listExams(Model model, HttpSession session) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) {
            return "redirect:/student/login";
        }

        List<Exam> allAvailableExams = examService.getAvailableExams();
        List<Exam> availableExams = new ArrayList<>();
        List<ExamWithAttemptInfo> completedExamsWithInfo = new ArrayList<>();

        List<ExamAttempt> studentAttempts = examService.getExamAttempts(student);
        Map<Long, ExamAttempt> attemptMap = studentAttempts.stream()
            .collect(Collectors.toMap(attempt -> attempt.getExam().getId(), attempt -> attempt));

        for (Exam exam : allAvailableExams) {
            if (attemptMap.containsKey(exam.getId())) {
                completedExamsWithInfo.add(new ExamWithAttemptInfo(exam, attemptMap.get(exam.getId())));
            } else {
                availableExams.add(exam);
            }
        }

        model.addAttribute("availableExams", availableExams);
        model.addAttribute("upcomingExams", examService.getUpcomingExams());
        model.addAttribute("completedExamsWithInfo", completedExamsWithInfo);
        // Add this line to provide completedExams for the template
        model.addAttribute("completedExams", completedExamsWithInfo.stream()
            .map(ExamWithAttemptInfo::getExam)
            .collect(Collectors.toList()));
        model.addAttribute("studentAttempts", studentAttempts);
        model.addAttribute("student", student);
        return "student/exam-list";
    }

    @GetMapping("/exams/{id}/start")
    public String startExam(@PathVariable Long id, HttpSession session, Model model) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) {
            return "redirect:/student/login";
        }
        try {
            Optional<Exam> examOpt = examService.getExamById(id);
            if (examOpt.isEmpty()) {
                model.addAttribute("error", "Exam not found!");
                return "redirect:/student/exams";
            }
            Exam exam = examOpt.get();
            if (!examService.canStudentAttemptExam(student, exam)) {
                model.addAttribute("error", "You cannot attempt this exam!");
                return "redirect:/student/exams";
            }
            ExamAttempt attempt = examService.startExam(student, exam);
            session.setAttribute("currentAttempt", attempt);
            return "redirect:/student/exams/" + id + "/attempt";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to start exam: " + e.getMessage());
            return "redirect:/student/exams";
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/exams/{id}/attempt")
    public String examAttempt(@PathVariable Long id, HttpSession session, Model model) {
        Student student = (Student) session.getAttribute("student");
        ExamAttempt attempt = (ExamAttempt) session.getAttribute("currentAttempt");

        if (student == null) {
            return "redirect:/student/login";
        }

        if (attempt == null || !attempt.getExam().getId().equals(id)) {
            return "redirect:/student/exams";
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return "redirect:/student/results/" + attempt.getId();
        }

        List<Question> questions = examService.getQuestionsByExam(id);
        List<Answer> answers = examService.getAttemptAnswers(attempt);

        model.addAttribute("exam", attempt.getExam());
        model.addAttribute("attempt", attempt);
        model.addAttribute("questions", questions);
        model.addAttribute("answers", answers);
        model.addAttribute("student", student);
        return "student/exam-attempt";
    }

    @PostMapping("/exams/{examId}/save-answer")
    @ResponseBody
    public Map<String, Object> saveAnswer(@PathVariable Long examId,
                                        @RequestParam Long questionId,
                                        @RequestParam String answer,
                                        HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Student student = (Student) session.getAttribute("student");
            ExamAttempt attempt = (ExamAttempt) session.getAttribute("currentAttempt");

            if (student == null || attempt == null) {
                response.put("success", false);
                response.put("message", "Session expired");
                return response;
            }

            if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
                response.put("success", false);
                response.put("message", "Exam is not active");
                return response;
            }

            attempt = examService.saveAnswer(attempt, questionId, answer);
            session.setAttribute("currentAttempt", attempt);

            response.put("success", true);
            response.put("message", "Answer saved successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to save answer: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/exams/{examId}/submit")
    public String submitExam(@PathVariable Long examId,
                           HttpSession session,
                           HttpServletRequest request,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Student student = (Student) session.getAttribute("student");
        ExamAttempt attempt = (ExamAttempt) session.getAttribute("currentAttempt");
        if (student == null) {
            return "redirect:/student/login";
        }
        if (attempt == null || !attempt.getExam().getId().equals(examId)) {
            return "redirect:/student/exams";
        }
        try {
            // Process form data - save all answers
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String paramName = entry.getKey();
                if (paramName.startsWith("question_")) {
                    String questionIdStr = paramName.substring("question_".length());
                    try {
                        Long questionId = Long.parseLong(questionIdStr);
                        String selectedAnswer = entry.getValue()[0];
                        // Save answer
                        examService.saveAnswer(attempt, questionId, selectedAnswer);
                    } catch (NumberFormatException e) {
                        // Invalid question ID, skip
                        continue;
                    }
                }
            }
            // Submit the exam
            attempt = examService.submitExam(attempt);
            // Remove from session
            session.removeAttribute("currentAttempt");
            redirectAttributes.addFlashAttribute("successMessage", "Exam submitted successfully!");
            return "redirect:/student/results/" + attempt.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Failed to submit exam: " + e.getMessage());
            return "redirect:/student/exams/" + examId + "/attempt";
        }
    }

    @PostMapping("/exams/window-switch")
    @ResponseBody
    public Map<String, Object> recordWindowSwitch(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            ExamAttempt attempt = (ExamAttempt) session.getAttribute("currentAttempt");
            if (attempt == null) {
                response.put("success", false);
                response.put("message", "No active exam");
                return response;
            }

            attempt = examService.recordWindowSwitch(attempt);
            session.setAttribute("currentAttempt", attempt);

            response.put("success", true);
            response.put("windowSwitchCount", attempt.getWindowSwitchCount());
            response.put("terminated", attempt.getStatus() == AttemptStatus.TERMINATED);

            if (attempt.getStatus() == AttemptStatus.TERMINATED) {
                session.removeAttribute("currentAttempt");
                response.put("message", "Exam terminated due to excessive window switching");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error recording window switch");
        }
        return response;
    }

    @PostMapping("/exams/update-time")
    @ResponseBody
    public Map<String, Object> updateTimeRemaining(@RequestParam Integer timeRemaining, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            ExamAttempt attempt = (ExamAttempt) session.getAttribute("currentAttempt");
            if (attempt == null) {
                response.put("success", false);
                return response;
            }

            attempt = examService.updateTimeRemaining(attempt, timeRemaining);
            session.setAttribute("currentAttempt", attempt);

            response.put("success", true);
            response.put("autoSubmitted", attempt.getStatus() == AttemptStatus.TIMED_OUT);

            if (attempt.getStatus() == AttemptStatus.TIMED_OUT) {
                session.removeAttribute("currentAttempt");
            }
        } catch (Exception e) {
            response.put("success", false);
        }
        return response;
    }

    @GetMapping("/results")
    public String viewResults(HttpSession session, Model model) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) {
            return "redirect:/student/login";
        }

        List<ExamAttempt> attempts = examService.getExamAttempts(student);
        model.addAttribute("attempts", attempts);
        model.addAttribute("student", student);
        return "student/results";
    }

    @GetMapping("/results/{attemptId}")
    public String viewResult(@PathVariable Long attemptId, HttpSession session, Model model) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) {
            return "redirect:/student/login";
        }

        Optional<ExamAttempt> attemptOpt = examService.getAttemptById(attemptId);
        if (attemptOpt.isEmpty() || !attemptOpt.get().getStudent().getId().equals(student.getId())) {
            return "redirect:/student/results";
        }

        ExamAttempt attempt = attemptOpt.get();
        List<Answer> answers = examService.getAttemptAnswers(attempt);

        model.addAttribute("attempt", attempt);
        model.addAttribute("answers", answers);
        model.addAttribute("student", student);
        return "student/result-detail";
    }

    @GetMapping("/")
    public String studentHome(Model model) {
        return "student/home";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("student", new Student());
        return "student/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute Student student,
                         @RequestParam(value = "dscFile", required = false) MultipartFile dscFile,
                         @RequestParam(value = "dscPassword", required = false) String dscPassword,
                         @RequestParam(value = "dscAlias", required = false) String dscAlias,
                         Model model, HttpSession session) {
        try {
            // Basic validation (keeping existing validation logic)
            if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
                model.addAttribute("error", "Student ID is required!");
                model.addAttribute("student", student);
                return "student/register";
            }
            if (student.getName() == null || student.getName().trim().isEmpty()) {
                model.addAttribute("error", "Name is required!");
                model.addAttribute("student", student);
                return "student/register";
            }
            if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
                model.addAttribute("error", "Email is required!");
                model.addAttribute("student", student);
                return "student/register";
            }
            if (student.getPassword() == null || student.getPassword().trim().isEmpty()) {
                model.addAttribute("error", "Password is required!");
                model.addAttribute("student", student);
                return "student/register";
            }

            // Clean and normalize data
            student.setStudentId(student.getStudentId().trim());
            student.setName(student.getName().trim());
            student.setEmail(student.getEmail().trim().toLowerCase());

            if (student.getOrganizationName() != null) {
                student.setOrganizationName(student.getOrganizationName().trim());
            }
            if (student.getOrganizationUnit() != null) {
                student.setOrganizationUnit(student.getOrganizationUnit().trim());
            }

            // Check for duplicates
            if (studentRepository.existsByStudentId(student.getStudentId())) {
                model.addAttribute("error", "Student ID already exists!");
                model.addAttribute("student", student);
                return "student/register";
            }
            if (studentRepository.existsByEmail(student.getEmail())) {
                model.addAttribute("error", "Email already exists!");
                model.addAttribute("student", student);
                return "student/register";
            }

            // Enhanced DSC handling with signature validation
            if (dscFile != null && !dscFile.isEmpty()) {
                if (dscPassword == null || dscPassword.trim().isEmpty()) {
                    model.addAttribute("error", "DSC password is required when uploading DSC file!");
                    model.addAttribute("student", student);
                    return "student/register";
                }

                try {
                    // Save DSC file
                    String dscFilePath = dscService.saveDSCFile(dscFile, student.getStudentId());

                    // Enhanced validation: Check if DSC can be used for signing
                    if (!dscService.validateDSC(dscFilePath, dscPassword)) {
                        dscService.deleteDSCFile(dscFilePath);
                        model.addAttribute("error", "Invalid DSC file or password!");
                        model.addAttribute("student", student);
                        return "student/register";
                    }

                    // Test signature functionality during registration
                    try {
                        String testChallenge = dscService.generateChallenge(student.getStudentId());
                        String testSignature = dscService.signChallenge(dscFilePath, dscPassword, testChallenge);
                        boolean verified = dscService.verifySignature(dscFilePath, dscPassword, testChallenge, testSignature);

                        if (!verified) {
                            dscService.deleteDSCFile(dscFilePath);
                            model.addAttribute("error", "DSC signature validation failed. Please ensure your certificate supports digital signing.");
                            model.addAttribute("student", student);
                            return "student/register";
                        }

                        System.out.println("DSC signature test successful during registration for: " + student.getStudentId());
                    } catch (Exception signatureException) {
                        dscService.deleteDSCFile(dscFilePath);
                        model.addAttribute("error", "DSC does not support digital signing: " + signatureException.getMessage());
                        model.addAttribute("student", student);
                        return "student/register";
                    }

                    student.setCertificatePath(dscFilePath);
                    if (dscAlias != null && !dscAlias.trim().isEmpty()) {
                        student.setCertificateAlias(dscAlias.trim());
                    }

                    // Encrypt and store DSC password
                    student.setDscPassword(dscEncryptionService.encryptDscPassword(dscPassword));
                    student.setHasDsc(true);
                    student.setCertificateCreatedDate(LocalDateTime.now());

                } catch (Exception e) {
                    model.addAttribute("error", "Failed to process DSC file: " + e.getMessage());
                    model.addAttribute("student", student);
                    return "student/register";
                }
            } else {
                // No DSC provided
                student.setHasDsc(false);
                student.setCertificatePath(null);
                student.setCertificateAlias(null);
                student.setDscPassword(null);
            }

            // Hash password before saving (but NOT DSC password)
            student.setPassword(passwordService.encodePassword(student.getPassword()));

            // Generate OTP and proceed with existing registration flow
            String otp = emailService.generateOTP();
            student.setOtp(otp);
            student.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
            student.setVerified(false);

            Student savedStudent = studentRepository.save(student);

            try {
                emailService.sendOTPEmail(student.getEmail(), otp, student.getName());
                session.setAttribute("pendingStudentId", savedStudent.getId());
                session.setAttribute("pendingStudentEmail", savedStudent.getEmail());

                String message = "Registration initiated! Please check your email for OTP verification.";
                if (student.isHasDsc()) {
                    message += " Your DSC has been validated and is ready for secure authentication.";
                }

                model.addAttribute("message", message);
                model.addAttribute("email", student.getEmail());
                return "student/verify-otp";

            } catch (Exception emailException) {
                studentRepository.deleteById(savedStudent.getId());
                if (student.isHasDsc()) {
                    dscService.deleteDSCFile(student.getCertificatePath());
                }
                model.addAttribute("error", "Failed to send verification email. Please try again later.");
                model.addAttribute("student", student);
                return "student/register";
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            model.addAttribute("student", student);
            return "student/register";
        }
    }

    @GetMapping("/verify-otp")
    public String showOtpVerification(Model model, HttpSession session) {
        Long pendingStudentId = (Long) session.getAttribute("pendingStudentId");
        String pendingStudentEmail = (String) session.getAttribute("pendingStudentEmail");

        if (pendingStudentId == null || pendingStudentEmail == null) {
            model.addAttribute("error", "Session expired. Please register again.");
            return "redirect:/student/register";
        }

        model.addAttribute("email", pendingStudentEmail);
        return "student/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp, Model model, HttpSession session) {
        Long pendingStudentId = (Long) session.getAttribute("pendingStudentId");
        if (pendingStudentId == null) {
            model.addAttribute("error", "Session expired. Please register again.");
            return "redirect:/student/register";
        }

        Optional<Student> studentOpt = studentRepository.findById(pendingStudentId);
        if (studentOpt.isEmpty()) {
            model.addAttribute("error", "Student not found. Please register again.");
            return "redirect:/student/register";
        }

        Student student = studentOpt.get();

        if (student.getOtp() != null && student.getOtp().equals(otp.trim()) &&
            student.getOtpExpiry() != null && LocalDateTime.now().isBefore(student.getOtpExpiry())) {

            student.setVerified(true);
            student.setOtp(null);
            student.setOtpExpiry(null);
            studentRepository.save(student);

            session.removeAttribute("pendingStudentId");
            session.removeAttribute("pendingStudentEmail");

            model.addAttribute("success", "Email verified successfully! You can now login.");
            return "student/login";
        } else {
            model.addAttribute("error", "Invalid or expired OTP. Please try again.");
            model.addAttribute("email", student.getEmail());
            return "student/verify-otp";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) {
            return "redirect:/student/login";
        }

        Optional<Student> refreshedStudent = studentRepository.findById(student.getId());
        if (refreshedStudent.isPresent()) {
            model.addAttribute("student", refreshedStudent.get());
        } else {
            return "redirect:/student/login";
        }
        return "student/dashboard";
    }

    @PostMapping("/resend-login-otp")
    public String resendLoginOtp(Model model, HttpSession session) {
        Long loginStudentId = (Long) session.getAttribute("loginStudentId");
        if (loginStudentId == null) {
            return "redirect:/student/login";
        }

        Optional<Student> studentOpt = studentRepository.findById(loginStudentId);
        if (studentOpt.isEmpty()) {
            return "redirect:/student/login";
        }

        Student student = studentOpt.get();
        try {
            String newOtp = emailService.generateOTP();
            student.setOtp(newOtp);
            student.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
            studentRepository.save(student);

            emailService.sendOTPEmail(student.getEmail(), newOtp, student.getName());
            model.addAttribute("message", "New OTP sent to your email!");
            model.addAttribute("email", student.getEmail());
            return "student/login-otp";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to resend OTP. Please try again.");
            model.addAttribute("email", student.getEmail());
            return "student/login-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(Model model, HttpSession session) {
        Long pendingStudentId = (Long) session.getAttribute("pendingStudentId");
        if (pendingStudentId == null) {
            return "redirect:/student/register";
        }

        Optional<Student> studentOpt = studentRepository.findById(pendingStudentId);
        if (studentOpt.isEmpty()) {
            return "redirect:/student/register";
        }

        Student student = studentOpt.get();
        try {
            String newOtp = emailService.generateOTP();
            student.setOtp(newOtp);
            student.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
            studentRepository.save(student);

            emailService.sendOTPEmail(student.getEmail(), newOtp, student.getName());

            model.addAttribute("message", "New OTP sent to your email!");
            model.addAttribute("email", student.getEmail());
            return "student/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to resend OTP. Please try again.");
            model.addAttribute("email", student.getEmail());
            return "student/verify-otp";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        return "student/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String studentId, @RequestParam String password, HttpSession session, Model model) {
        try {
            Optional<Student> studentOpt = studentRepository.findByStudentId(studentId.trim());
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                if (!student.isVerified()) {
                    model.addAttribute("error", "Please verify your email first! Check your email for OTP.");
                    return "student/login";
                }

                // Verify password first
                if (!passwordService.matches(password, student.getPassword())) {
                    model.addAttribute("error", "Invalid Student ID or Password!");
                    return "student/login";
                }

                // Check if individual auth method is set for this student
                AuthConfig.AuthMethod authMethodToUse = null;

                if (student.isAuthMethodSetByAdmin() && student.getPreferredAuthMethod() != null) {
                    // Use individual student's auth method
                    authMethodToUse = student.getPreferredAuthMethod();
                } else {
                    // Use global auth method
                    Optional<AuthConfig> authConfigOpt = authConfigRepository.findActiveAuthConfig();
                    if (authConfigOpt.isPresent()) {
                        authMethodToUse = authConfigOpt.get().getAuthMethod();
                    } else {
                        authMethodToUse = AuthConfig.AuthMethod.PASSWORD_ONLY; // Default
                    }
                }

                // Process based on auth method
                switch (authMethodToUse) {
                    case PASSWORD_ONLY:
                        session.setAttribute("student", student);
                        return "redirect:/student/dashboard";

                    case OTP:
                        // Generate OTP for login
                        String otp = emailService.generateOTP();
                        student.setOtp(otp);
                        student.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
                        studentRepository.save(student);

                        try {
                            emailService.sendOTPEmail(student.getEmail(), otp, student.getName());
                            session.setAttribute("loginStudentId", student.getId());
                            model.addAttribute("message", "OTP sent to your email for login verification.");
                            model.addAttribute("email", student.getEmail());
                            return "student/login-otp";
                        } catch (Exception emailException) {
                            model.addAttribute("error", "Failed to send OTP. Please try again.");
                            return "student/login";
                        }

                    case DSC:
                        if (!student.isHasDsc()) {
                            model.addAttribute("error", "DSC authentication required but you don't have DSC registered!");
                            return "student/login";
                        }
                        session.setAttribute("loginStudentId", student.getId());
                        model.addAttribute("student", student);
                        return "student/login-dsc";

                    default:
                        session.setAttribute("student", student);
                        return "redirect:/student/dashboard";
                }
            } else {
                model.addAttribute("error", "Invalid Student ID or Password!");
                return "student/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Login failed. Please try again.");
            return "student/login";
        }
    }

    @GetMapping("/login-dsc")
    public String showLoginDsc(Model model, HttpSession session) {
        Long loginStudentId = (Long) session.getAttribute("loginStudentId");
        if (loginStudentId == null) {
            return "redirect:/student/login";
        }

        Optional<Student> studentOpt = studentRepository.findById(loginStudentId);
        if (studentOpt.isPresent()) {
            model.addAttribute("student", studentOpt.get());
        }
        return "student/login-dsc";
    }

    @PostMapping("/login-dsc")
    public String verifyLoginDsc(@RequestParam("dscFile") MultipartFile dscFile,
     @RequestParam String dscPassword,
     HttpSession session,
     Model model) {

     Long loginStudentId = (Long) session.getAttribute("loginStudentId");
     if (loginStudentId == null) {
     return "redirect:/student/login";
     }
     Optional<Student> studentOpt = studentRepository.findById(loginStudentId);
     if (studentOpt.isEmpty()) {
     return "redirect:/student/login";
     }
     Student student = studentOpt.get();

     // Check if student has registered DSC
     if (student.getCertificatePath() == null || student.getDscPassword() == null) {
     model.addAttribute("error", "No DSC certificate registered for this account!");
     model.addAttribute("student", student);
     return "student/login-dsc";
     }
     // Validate uploaded file
     if (dscFile == null || dscFile.isEmpty()) {
     model.addAttribute("error", "Please upload your DSC file!");
     model.addAttribute("student", student);
     return "student/login-dsc";
     }
     String tempDscPath = null;
     try {
     // Save uploaded DSC file temporarily
     tempDscPath = dscService.saveTempDSCFile(dscFile, student.getStudentId() + "_login");

     // Validate uploaded DSC with provided password
     if (!dscService.validateDSC(tempDscPath, dscPassword)) {
     model.addAttribute("error", "Invalid DSC file or password! Please check your certificate and password.");
     model.addAttribute("student", student);
     return "student/login-dsc";
     }
     // Get stored DSC password and decrypt it
     String storedDscPassword = dscEncryptionService.decryptDscPassword(student.getDscPassword());
     // Validate stored DSC is still accessible
     if (!dscService.validateDSC(student.getCertificatePath(), storedDscPassword)) {
     model.addAttribute("error", "Your registered DSC certificate is invalid or corrupted. Please contact support.");
     model.addAttribute("student", student);
     return "student/login-dsc";
     }
     // NEW: Use advanced DSC authentication with digital signatures
     boolean authenticated = dscService.authenticateDSCAdvanced(
     student.getCertificatePath(), // registered DSC path
     tempDscPath, // uploaded DSC path
     storedDscPassword, // registered DSC password
     dscPassword, // uploaded DSC password
     student.getStudentId() // student ID for challenge
     );
     if (!authenticated) {
     model.addAttribute("error", "DSC authentication failed! The certificate does not match or signature verification failed.");
     model.addAttribute("student", student);
     return "student/login-dsc";
     }
     // Success - DSC authentication complete
     session.removeAttribute("loginStudentId");
     session.setAttribute("student", student);
     return "redirect:/student/dashboard";
     } catch (Exception e) {
     System.err.println("DSC authentication error: " + e.getMessage());
     e.printStackTrace();
     model.addAttribute("error", "DSC authentication failed. Please try again.");
     model.addAttribute("student", student);
     return "student/login-dsc";
     } finally {
     // Clean up temporary file
     if (tempDscPath != null) {
     dscService.deleteDSCFile(tempDscPath);
     }
     }
    }
    // Optional: Add endpoint for testing DSC signature functionality
    @PostMapping("/test-dsc-signature")
    @ResponseBody
    public Map<String, Object> testDscSignature(@RequestParam("dscFile") MultipartFile dscFile,
     @RequestParam String dscPassword,
     HttpSession session) {
     Map<String, Object> response = new HashMap<>();

     Student student = (Student) session.getAttribute("student");
     if (student == null) {
     response.put("success", false);
     response.put("message", "Not logged in");
     return response;
     }
     String tempDscPath = null;
     try {
     // Save uploaded DSC temporarily
     tempDscPath = dscService.saveTempDSCFile(dscFile, student.getStudentId() + "_test");

     // Validate DSC
     if (!dscService.validateDSC(tempDscPath, dscPassword)) {
     response.put("success", false);
     response.put("message", "Invalid DSC file or password");
     return response;
     }
     // Generate challenge and create signature
     String challenge = dscService.generateChallenge(student.getStudentId());
     String signature = dscService.signChallenge(tempDscPath, dscPassword, challenge);

     response.put("success", true);
     response.put("challenge", challenge);
     response.put("signature", signature);
     response.put("message", "DSC signature test successful");

     } catch (Exception e) {
     response.put("success", false);
     response.put("message", "DSC signature test failed: " + e.getMessage());
     } finally {
     if (tempDscPath != null) {
     dscService.deleteDSCFile(tempDscPath);
     }
     }

     return response;
    }
     @GetMapping("/logout")
     public String logout(HttpSession session) {
     session.invalidate();
     return "redirect:/student/";
     }
     @GetMapping("/forgot-password")
     public String forgotPasswordPage() {
     return "student/forgot-password";
     }
     @GetMapping("/login-otp")
     public String showLoginOtp(Model model, HttpSession session) {
     Long loginStudentId = (Long) session.getAttribute("loginStudentId");
     if (loginStudentId == null) {
     return "redirect:/student/login";
     }

     Optional<Student> studentOpt = studentRepository.findById(loginStudentId);
     if (studentOpt.isPresent()) {
     model.addAttribute("email", studentOpt.get().getEmail());
     }
     return "student/login-otp";
     }
     @PostMapping("/login-otp")
     public String verifyLoginOtp(@RequestParam String otp, HttpSession session, Model model) {
     Long loginStudentId = (Long) session.getAttribute("loginStudentId");
     if (loginStudentId == null) {
     return "redirect:/student/login";
     }
     Optional<Student> studentOpt = studentRepository.findById(loginStudentId);
     if (studentOpt.isEmpty()) {
     return "redirect:/student/login";
     }
     Student student = studentOpt.get();

     if (student.getOtp() != null && student.getOtp().equals(otp.trim()) &&
     student.getOtpExpiry() != null && LocalDateTime.now().isBefore(student.getOtpExpiry())) {

     student.setOtp(null);
     student.setOtpExpiry(null);
     studentRepository.save(student);

     session.removeAttribute("loginStudentId");
     session.setAttribute("student", student);
     return "redirect:/student/dashboard";
     } else {
     model.addAttribute("error", "Invalid or expired OTP. Please try again.");
     model.addAttribute("email", student.getEmail());
     return "student/login-otp";
     }
     }

     @PostMapping("/forgot-password")
     public String forgotPassword(@RequestParam String email, Model model) {
     try {
     Optional<Student> studentOpt = studentRepository.findByEmail(email.trim().toLowerCase());
     if (studentOpt.isPresent()) {
     Student student = studentOpt.get();

     if (!student.isVerified()) {
     model.addAttribute("error", "Please verify your email first!");
     return "student/forgot-password";
     }

     String newPassword = UUID.randomUUID().toString().substring(0, 8);
     // Hash the new password before saving
     student.setPassword(passwordService.encodePassword(newPassword));
     studentRepository.save(student);

     emailService.sendResetPasswordEmail(email, newPassword);
     model.addAttribute("message", "New password has been sent to your email!");
     } else {
     model.addAttribute("error", "Email not found in our records!");
     }
     } catch (Exception e) {
     model.addAttribute("error", "Failed to process request. Please try again.");
     }
     return "student/forgot-password";
     }
    }