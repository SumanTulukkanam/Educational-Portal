package com.example.demo.controller;
import java.util.List;
import java.util.ArrayList;
import com.example.demo.model.Admin;
import com.example.demo.service.ExamService;
import com.example.demo.service.EmailService;
import com.example.demo.service.PasswordService;
import com.example.demo.model.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.example.demo.model.Student;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import com.example.demo.model.AttemptStatus;
import com.example.demo.model.ExamAttempt;
import com.example.demo.model.AuthConfig;
import com.example.demo.repository.AuthConfigRepository;
@Controller
@RequestMapping("/admin")
public class AdminController {


 @Autowired
 private AuthConfigRepository authConfigRepository;
 @Autowired
 private AdminRepository adminRepository;
 @Autowired
 private ExamService examService;
 @Autowired
 private EmailService emailService;
 @Autowired
 private StudentRepository studentRepository;
 @Autowired
 private PasswordService passwordService;
 @GetMapping("/")
 public String adminHome() {
 return "redirect:/admin/login";
 }

 @GetMapping("/auth-config")
 public String showAuthConfig(HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 // Get current active auth config
 Optional<AuthConfig> currentConfig = authConfigRepository.findActiveAuthConfig();
 model.addAttribute("currentConfig", currentConfig.orElse(null));
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());

 return "admin/auth-config";
 }
 @PostMapping("/auth-config")
 public String updateAuthConfig(@RequestParam("authMethod") AuthConfig.AuthMethod authMethod,
 @RequestParam(value = "description", required = false) String description,
 HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 try {
 // Deactivate all existing configurations
 List<AuthConfig> allConfigs = authConfigRepository.findAll();
 for (AuthConfig config : allConfigs) {
 config.setActive(false);
 authConfigRepository.save(config);
 }

 // Create new active configuration
 AuthConfig newConfig = new AuthConfig();
 newConfig.setAuthMethod(authMethod);
 newConfig.setActive(true);
 newConfig.setDescription(description != null ? description : authMethod.getDisplayName());
 authConfigRepository.save(newConfig);

 model.addAttribute("success", "Authentication method updated successfully!");
 model.addAttribute("currentConfig", newConfig);
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());

 } catch (Exception e) {
 model.addAttribute("error", "Failed to update authentication method: " + e.getMessage());
 Optional<AuthConfig> currentConfig = authConfigRepository.findActiveAuthConfig();
 model.addAttribute("currentConfig", currentConfig.orElse(null));
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());
 }

 return "admin/auth-config";
 }
 // New method for Student Login Type Management
 @GetMapping("/student-login-config")
 public String showStudentLoginConfig(HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 // Get current active auth config for students
 Optional<AuthConfig> currentConfig = authConfigRepository.findActiveAuthConfig();
 model.addAttribute("currentConfig", currentConfig.orElse(null));
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());
 model.addAttribute("admin", admin);

 return "admin/student-login-config";
 }
 @PostMapping("/student-login-config")
 public String updateStudentLoginConfig(@RequestParam("authMethod") AuthConfig.AuthMethod authMethod,
 @RequestParam(value = "description", required = false) String description,
 HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 try {
 // Deactivate all existing configurations
 List<AuthConfig> allConfigs = authConfigRepository.findAll();
 for (AuthConfig config : allConfigs) {
 config.setActive(false);
 authConfigRepository.save(config);
 }

 // Create new active configuration for students
 AuthConfig newConfig = new AuthConfig();
 newConfig.setAuthMethod(authMethod);
 newConfig.setActive(true);
 newConfig.setDescription(description != null ? description : "Student Login: " + authMethod.getDisplayName());
 authConfigRepository.save(newConfig);

 model.addAttribute("success", "Student login method updated successfully! Students will now use " + authMethod.getDisplayName() + " authentication.");
 model.addAttribute("currentConfig", newConfig);
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());
 model.addAttribute("admin", admin);

 } catch (Exception e) {
 model.addAttribute("error", "Failed to update student login method: " + e.getMessage());
 Optional<AuthConfig> currentConfig = authConfigRepository.findActiveAuthConfig();
 model.addAttribute("currentConfig", currentConfig.orElse(null));
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());
 model.addAttribute("admin", admin);
 }

 return "admin/student-login-config";
 }
 // Method to get current student login type (for use in other parts of the application)
 @GetMapping("/api/current-student-login-type")
 @ResponseBody
 public String getCurrentStudentLoginType(HttpSession session) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "UNAUTHORIZED";
 }

 Optional<AuthConfig> currentConfig = authConfigRepository.findActiveAuthConfig();
 if (currentConfig.isPresent()) {
 return currentConfig.get().getAuthMethod().name();
 }
 return "REGULAR"; // Default to regular login
 }

 @GetMapping("/login")
 public String showLoginForm(Model model) {
 return "admin/login";
 }

 @PostMapping("/login")
 public String login(@RequestParam String username, // or adminId
 @RequestParam String password,
 HttpSession session, Model model) {
 try {
 // Look up admin instead of student
 Optional<Admin> adminOpt = adminRepository.findByUsername(username.trim());
 if (adminOpt.isPresent()) {
 Admin admin = adminOpt.get();

 if (!passwordService.matches(password, admin.getPassword())) {
 model.addAttribute("error", "Invalid credentials!");
 return "admin/login";
 }

 session.setAttribute("admin", admin);
 return "redirect:/admin/dashboard";
 } else {
 model.addAttribute("error", "Invalid credentials!");
 return "admin/login";
 }
 } catch (Exception e) {
 model.addAttribute("error", "Login failed. Please try again.");
 return "admin/login";
 }
 }


 // Exam Management
 @GetMapping("/exams")
 public String listExams(HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 // Get all active exams
 List<Exam> allExams = examService.getAllActiveExams();

 // For admin view, provide all expected variables
 // You can categorize exams if needed, or just use empty lists for unused categories
 model.addAttribute("exams", allExams);
 model.addAttribute("availableExams", allExams);
 model.addAttribute("upcomingExams", new ArrayList<Exam>()); // Empty list or filter from allExams
 model.addAttribute("completedExams", new ArrayList<Exam>()); // Empty list or add completed exams
 model.addAttribute("studentAttempts", new ArrayList<ExamAttempt>()); // Empty list for admin view
 model.addAttribute("admin", admin);

 return "admin/exam-list";
 }
 @GetMapping("/exams/create-exam")
 public String showCreateExamForm(HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 model.addAttribute("exam", new Exam());
 model.addAttribute("admin", admin);
 return "admin/create-exam";
 }
 @PostMapping("/exams/create")
 public String createExam(@ModelAttribute Exam exam, HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 try {
 // Enhanced validation
 if (exam.getTitle() == null || exam.getTitle().trim().isEmpty()) {
 model.addAttribute("error", "Exam title is required!");
 model.addAttribute("exam", exam);
 model.addAttribute("admin", admin);
 return "admin/create-exam";
 }

 if (exam.getDuration() == null || exam.getDuration() <= 0) {
 model.addAttribute("error", "Valid duration is required!");
 model.addAttribute("exam", exam);
 model.addAttribute("admin", admin);
 return "admin/create-exam";
 }

 if (exam.getStartTime() == null || exam.getEndTime() == null) {
 model.addAttribute("error", "Start time and end time are required!");
 model.addAttribute("exam", exam);
 model.addAttribute("admin", admin);
 return "admin/create-exam";
 }

 // Check if start time is not in the past
 if (exam.getStartTime().isBefore(LocalDateTime.now())) {
 model.addAttribute("error", "Start time cannot be in the past!");
 model.addAttribute("exam", exam);
 model.addAttribute("admin", admin);
 return "admin/create-exam";
 }

 if (exam.getStartTime().isAfter(exam.getEndTime())) {
 model.addAttribute("error", "Start time must be before end time!");
 model.addAttribute("exam", exam);
 model.addAttribute("admin", admin);
 return "admin/create-exam";
 }

 if (exam.getTotalMarks() == null || exam.getTotalMarks() <= 0) {
 model.addAttribute("error", "Valid total marks is required!");
 model.addAttribute("exam", exam);
 model.addAttribute("admin", admin);
 return "admin/create-exam";
 }

 if (exam.getPassingMarks() == null || exam.getPassingMarks() < 0 ||
 exam.getPassingMarks() > exam.getTotalMarks()) {
 model.addAttribute("error", "Passing marks must be between 0 and total marks!");
 model.addAttribute("exam", exam);
 model.addAttribute("admin", admin);
 return "admin/create-exam";
 }
 exam.setActive(true);
 if (exam.getMaxAttempts() == null || exam.getMaxAttempts() <= 0) {
 exam.setMaxAttempts(1);
 }
 Exam savedExam = examService.saveExam(exam);
 return "redirect:/admin/exams/" + savedExam.getId() + "/questions?success=Exam created successfully";

 } catch (Exception e) {
 model.addAttribute("error", "Failed to create exam: " + e.getMessage());
 model.addAttribute("exam", exam);
 model.addAttribute("admin", admin);
 return "admin/create-exam";
 }
 }
 @GetMapping("/exams/{examId}/questions")
 public String manageQuestions(@PathVariable Long examId, HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 Optional<Exam> examOpt = examService.getExamById(examId);
 if (examOpt.isEmpty()) {
 return "redirect:/admin/exams";
 }

 Exam exam = examOpt.get();
 List<Question> questions = examService.getQuestionsByExam(examId);

 model.addAttribute("exam", exam);
 model.addAttribute("questions", questions);
 model.addAttribute("newQuestion", new Question());
 model.addAttribute("admin", admin);

 return "admin/manage-questions";
 }
 @PostMapping("/exams/{examId}/questions/add")
 public String addQuestion(@PathVariable Long examId, @ModelAttribute Question question,
 HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 try {
 Optional<Exam> examOpt = examService.getExamById(examId);
 if (examOpt.isEmpty()) {
 return "redirect:/admin/exams";
 }

 Exam exam = examOpt.get();

 // Validation
 if (question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
 model.addAttribute("error", "Question text is required!");
 return "redirect:/admin/exams/" + examId + "/questions?error=Question text is required";
 }

 if (question.getOptionA() == null || question.getOptionA().trim().isEmpty() ||
 question.getOptionB() == null || question.getOptionB().trim().isEmpty() ||
 question.getOptionC() == null || question.getOptionC().trim().isEmpty() ||
 question.getOptionD() == null || question.getOptionD().trim().isEmpty()) {
 return "redirect:/admin/exams/" + examId + "/questions?error=All options are required";
 }

 if (question.getCorrectAnswer() == null || question.getCorrectAnswer().trim().isEmpty()) {
 return "redirect:/admin/exams/" + examId + "/questions?error=Correct answer is required";
 }

 if (!question.getCorrectAnswer().matches("[ABCD]")) {
 return "redirect:/admin/exams/" + examId + "/questions?error=Correct answer must be A, B, C, or D";
 }

 question.setExam(exam);
 if (question.getMarks() == null || question.getMarks() <= 0) {
 question.setMarks(1);
 }

 List<Question> existingQuestions = examService.getQuestionsByExam(examId);
 question.setQuestionNumber(existingQuestions.size() + 1);

 examService.saveQuestion(question);
 return "redirect:/admin/exams/" + examId + "/questions?success=Question added successfully";

 } catch (Exception e) {
 return "redirect:/admin/exams/" + examId + "/questions?error=Failed to add question";
 }
 }
 @PostMapping("/exams/{examId}/questions/{questionId}/delete")
 public String deleteQuestion(@PathVariable Long examId, @PathVariable Long questionId,
 HttpSession session) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 try {
 examService.deleteQuestion(questionId);
 } catch (Exception e) {
 // Log error but continue
 }

 return "redirect:/admin/exams/" + examId + "/questions";
 }
 @GetMapping("/exams/{examId}/results")
 public String viewExamResults(@PathVariable Long examId, HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 Optional<Exam> examOpt = examService.getExamById(examId);
 if (examOpt.isEmpty()) {
 return "redirect:/admin/exams";
 }

 Exam exam = examOpt.get();
 List<ExamAttempt> attempts = examService.getExamAttempts(exam);

 model.addAttribute("exam", exam);
 model.addAttribute("attempts", attempts);
 model.addAttribute("totalAttempts", attempts.size());
 model.addAttribute("completedAttempts", examService.getCompletedAttemptsCount(exam));
 model.addAttribute("averageScore", examService.getAverageScore(exam));
 model.addAttribute("admin", admin);

 return "admin/exam-results";
 }
 @PostMapping("/exams/{examId}/toggle-status")
 public String toggleExamStatus(@PathVariable Long examId, HttpSession session) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 try {
 Optional<Exam> examOpt = examService.getExamById(examId);
 if (examOpt.isPresent()) {
 Exam exam = examOpt.get();
 exam.setActive(!exam.isActive());
 examService.saveExam(exam);
 }
 } catch (Exception e) {
 // Log error but continue
 }

 return "redirect:/admin/exams";
 }
 @PostMapping("/exams/{examId}/delete")
 public String deleteExam(@PathVariable Long examId, HttpSession session) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 try {
 examService.deleteExam(examId);
 } catch (Exception e) {
 // Log error but continue
 }

 return "redirect:/admin/exams";
 }
 @GetMapping("/edit-student/{id}")
 public String showEditStudentForm(@PathVariable Long id, HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 Optional<Student> studentOpt = studentRepository.findById(id);
 if (studentOpt.isEmpty()) {
 return "redirect:/admin/dashboard?error=Student not found";
 }

 model.addAttribute("student", studentOpt.get());
 model.addAttribute("admin", admin);
 return "admin/edit-student";
 }
 @PostMapping("/edit-student/{id}")
 public String editStudent(@PathVariable Long id, @ModelAttribute Student updatedStudent,
 HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 try {
 Optional<Student> existingStudentOpt = studentRepository.findById(id);
 if (existingStudentOpt.isEmpty()) {
 model.addAttribute("error", "Student not found!");
 return "redirect:/admin/dashboard";
 }

 Student existingStudent = existingStudentOpt.get();

 if (updatedStudent.getStudentId() == null || updatedStudent.getStudentId().trim().isEmpty()) {
 model.addAttribute("error", "Student ID is required!");
 model.addAttribute("student", existingStudent);
 model.addAttribute("admin", admin);
 return "admin/edit-student";
 }

 if (updatedStudent.getName() == null || updatedStudent.getName().trim().isEmpty()) {
 model.addAttribute("error", "Name is required!");
 model.addAttribute("student", existingStudent);
 model.addAttribute("admin", admin);
 return "admin/edit-student";
 }

 if (updatedStudent.getEmail() == null || updatedStudent.getEmail().trim().isEmpty()) {
 model.addAttribute("error", "Email is required!");
 model.addAttribute("student", existingStudent);
 model.addAttribute("admin", admin);
 return "admin/edit-student";
 }

 if (updatedStudent.getPassword() == null || updatedStudent.getPassword().trim().isEmpty()) {
 model.addAttribute("error", "Password is required!");
 model.addAttribute("student", existingStudent);
 model.addAttribute("admin", admin);
 return "admin/edit-student";
 }

 updatedStudent.setStudentId(updatedStudent.getStudentId().trim());
 updatedStudent.setName(updatedStudent.getName().trim());
 updatedStudent.setEmail(updatedStudent.getEmail().trim().toLowerCase());

 // Check for duplicates
 Optional<Student> duplicateStudentId = studentRepository.findByStudentId(updatedStudent.getStudentId());
 if (duplicateStudentId.isPresent() && !duplicateStudentId.get().getId().equals(id)) {
 model.addAttribute("error", "Student ID already exists for another student!");
 model.addAttribute("student", existingStudent);
 model.addAttribute("admin", admin);
 return "admin/edit-student";
 }

 Optional<Student> duplicateEmail = studentRepository.findByEmail(updatedStudent.getEmail());
 if (duplicateEmail.isPresent() && !duplicateEmail.get().getId().equals(id)) {
 model.addAttribute("error", "Email already exists for another student!");
 model.addAttribute("student", existingStudent);
 model.addAttribute("admin", admin);
 return "admin/edit-student";
 }

 existingStudent.setStudentId(updatedStudent.getStudentId());
 existingStudent.setName(updatedStudent.getName());
 existingStudent.setEmail(updatedStudent.getEmail());

 // Only update password if it's different (encrypt it)
 if (!passwordService.matches(updatedStudent.getPassword(), existingStudent.getPassword())) {
 existingStudent.setPassword(passwordService.encodePassword(updatedStudent.getPassword()));
 }

 studentRepository.save(existingStudent);

 model.addAttribute("success", "Student updated successfully!");
 model.addAttribute("student", existingStudent);
 model.addAttribute("admin", admin);
 return "admin/edit-student";

 } catch (Exception e) {
 System.err.println("Error updating student: " + e.getMessage());
 e.printStackTrace();

 Optional<Student> originalStudent = studentRepository.findById(id);
 if (originalStudent.isPresent()) {
 model.addAttribute("student", originalStudent.get());
 }
 model.addAttribute("error", "Failed to update student. Please try again.");
 model.addAttribute("admin", admin);
 return "admin/edit-student";
 }
 }
 @PostMapping("/delete-student/{id}")
 public String deleteStudent(@PathVariable Long id, HttpSession session) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }

 try {
 studentRepository.deleteById(id);
 } catch (Exception e) {
 System.err.println("Error deleting student: " + e.getMessage());
 }

 return "redirect:/admin/dashboard";
 }

 @GetMapping("/students/{studentId}/auth-method")
 public String showStudentAuthMethod(@PathVariable Long studentId, HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }
 Optional<Student> studentOpt = studentRepository.findById(studentId);
 if (studentOpt.isEmpty()) {
 return "redirect:/admin/dashboard?error=Student not found";
 }
 Student student = studentOpt.get();
 model.addAttribute("student", student);
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());
 model.addAttribute("admin", admin);

 return "admin/student-auth-method";
 }
 @PostMapping("/students/{studentId}/auth-method")
 public String setStudentAuthMethod(@PathVariable Long studentId,
 @RequestParam("authMethod") AuthConfig.AuthMethod authMethod,
 HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }
 try {
 Optional<Student> studentOpt = studentRepository.findById(studentId);
 if (studentOpt.isEmpty()) {
 return "redirect:/admin/dashboard?error=Student not found";
 }
 Student student = studentOpt.get();

 // Validate if DSC is selected but student doesn't have DSC
 if (authMethod == AuthConfig.AuthMethod.DSC && !student.isHasDsc()) {
 model.addAttribute("error", "Cannot set DSC authentication - Student doesn't have DSC registered!");
 model.addAttribute("student", student);
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());
 model.addAttribute("admin", admin);
 return "admin/student-auth-method";
 }
 student.setPreferredAuthMethod(authMethod);
 student.setAuthMethodSetByAdmin(true);
 studentRepository.save(student);
 model.addAttribute("success", "Authentication method updated successfully for " + student.getName());
 model.addAttribute("student", student);
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());
 model.addAttribute("admin", admin);

 return "admin/student-auth-method";
 } catch (Exception e) {
 model.addAttribute("error", "Failed to update authentication method: " + e.getMessage());
 Optional<Student> student = studentRepository.findById(studentId);
 if (student.isPresent()) {
 model.addAttribute("student", student.get());
 }
 model.addAttribute("authMethods", AuthConfig.AuthMethod.values());
 model.addAttribute("admin", admin);
 return "admin/student-auth-method";
 }
 }
 @GetMapping("/dashboard")
 public String dashboard(HttpSession session, Model model) {
 Admin admin = (Admin) session.getAttribute("admin");
 if (admin == null) {
 return "redirect:/admin/login";
 }
 // Get current student login configuration
 Optional<AuthConfig> currentConfig = authConfigRepository.findActiveAuthConfig();
 String currentLoginType = "Regular Login";
 if (currentConfig.isPresent()) {
 currentLoginType = currentConfig.get().getAuthMethod().getDisplayName();
 }
 List<Student> students = studentRepository.findAll();

 model.addAttribute("admin", admin);
 model.addAttribute("students", students);
 model.addAttribute("totalStudents", studentRepository.count());
 model.addAttribute("totalExams", examService.getAllActiveExams().size());
 model.addAttribute("currentStudentLoginType", currentLoginType);

 return "admin/dashboard";
 }
 @GetMapping("/logout")
 public String logout(HttpSession session) {
 session.invalidate();
 return "redirect:/admin/login";
 }
}