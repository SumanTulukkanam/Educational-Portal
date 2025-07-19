package com.example.demo.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer timeRemaining; // in seconds

    @Column(nullable = false)
    private Integer obtainedMarks = 0;

    @Column(nullable = false)
    private Integer totalQuestions = 0;

    @Column(nullable = false)
    private Integer correctAnswers = 0;

    @Column(nullable = false)
    private Integer wrongAnswers = 0;

    @Column(nullable = false)
    private Integer unansweredQuestions = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(nullable = false)
    private Integer windowSwitchCount = 0;

    @Column(nullable = false)
    private boolean autoSubmitted = false;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "examAttempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Answer> answers;

    // Constructors
    public ExamAttempt() {}

    public ExamAttempt(Student student, Exam exam, Integer timeRemaining) {
        this.student = student;
        this.exam = exam;
        this.startTime = LocalDateTime.now();
        this.timeRemaining = timeRemaining;
    }

    // Getters and Setters
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }

    public Student getStudent() { 
        return student; 
    }
    
    public void setStudent(Student student) { 
        this.student = student; 
    }

    public Exam getExam() { 
        return exam; 
    }
    
    public void setExam(Exam exam) { 
        this.exam = exam; 
    }

    public LocalDateTime getStartTime() { 
        return startTime; 
    }
    
    public void setStartTime(LocalDateTime startTime) { 
        this.startTime = startTime; 
    }

    public LocalDateTime getEndTime() { 
        return endTime; 
    }
    
    public void setEndTime(LocalDateTime endTime) { 
        this.endTime = endTime; 
    }

    public Integer getTimeRemaining() { 
        return timeRemaining; 
    }
    
    public void setTimeRemaining(Integer timeRemaining) { 
        this.timeRemaining = timeRemaining;
    }

    public Integer getObtainedMarks() { 
        return obtainedMarks; 
    }
    
    public void setObtainedMarks(Integer obtainedMarks) { 
        this.obtainedMarks = obtainedMarks;
    }

    public Integer getTotalQuestions() { 
        return totalQuestions; 
    }
    
    public void setTotalQuestions(Integer totalQuestions) { 
        this.totalQuestions = totalQuestions; 
    }

    public Integer getCorrectAnswers() { 
        return correctAnswers; 
    }
    
    public void setCorrectAnswers(Integer correctAnswers) { 
        this.correctAnswers = correctAnswers; 
    }

    public Integer getWrongAnswers() { 
        return wrongAnswers; 
    }
    
    public void setWrongAnswers(Integer wrongAnswers) { 
        this.wrongAnswers = wrongAnswers; 
    }

    public Integer getUnansweredQuestions() { 
        return unansweredQuestions; 
    }
    
    public void setUnansweredQuestions(Integer unansweredQuestions) {
        this.unansweredQuestions = unansweredQuestions; 
    }

    public AttemptStatus getStatus() { 
        return status; 
    }
    
    public void setStatus(AttemptStatus status) { 
        this.status = status; 
    }

    public Integer getWindowSwitchCount() { 
        return windowSwitchCount; 
    }
    
    public void setWindowSwitchCount(Integer windowSwitchCount) { 
        this.windowSwitchCount = windowSwitchCount; 
    }

    public boolean isAutoSubmitted() { 
        return autoSubmitted; 
    }
    
    public void setAutoSubmitted(boolean autoSubmitted) { 
        this.autoSubmitted = autoSubmitted;
    }

    public String getRemarks() { 
        return remarks; 
    }
    
    public void setRemarks(String remarks) { 
        this.remarks = remarks; 
    }

    public List<Answer> getAnswers() { 
        return answers; 
    }
    
    public void setAnswers(List<Answer> answers) { 
        this.answers = answers; 
    }

    // Helper methods
    public double getPercentage() {
        if (exam == null || exam.getTotalMarks() == null || exam.getTotalMarks() == 0) {
            return 0.0;
        }
        return Math.round((double) obtainedMarks / exam.getTotalMarks() * 100.0 * 100.0) / 100.0;
    }
    
    public boolean isPassed() {
        if (exam == null || exam.getPassingMarks() == null) {
            return false;
        }
        return obtainedMarks >= exam.getPassingMarks();
    }
}