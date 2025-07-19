package com.example.demo.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer duration; // in minutes

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer totalMarks;

    @Column(nullable = false)
    private Integer passingMarks;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean windowSwitchingAllowed = false;

    @Column(nullable = false)
    private Integer maxAttempts = 1;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Question> questions;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExamAttempt> examAttempts;

    // Constructors
    public Exam() {}

    public Exam(String title, String description, Integer duration, LocalDateTime startTime, LocalDateTime endTime, Integer totalMarks, Integer passingMarks) {
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalMarks = totalMarks;
        this.passingMarks = passingMarks;
    }

    // Getters and Setters
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }

    public String getTitle() { 
        return title; 
    }
    
    public void setTitle(String title) { 
        this.title = title; 
    }

    public String getDescription() { 
        return description; 
    }
    
    public void setDescription(String description) { 
        this.description = description; 
    }

    public Integer getDuration() { 
        return duration; 
    }
    
    public void setDuration(Integer duration) { 
        this.duration = duration; 
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

    public Integer getTotalMarks() { 
        return totalMarks; 
    }
    
    public void setTotalMarks(Integer totalMarks) { 
        this.totalMarks = totalMarks; 
    }

    public Integer getPassingMarks() { 
        return passingMarks; 
    }
    
    public void setPassingMarks(Integer passingMarks) { 
        this.passingMarks = passingMarks; 
    }

    public boolean isActive() { 
        return active; 
    }
    
    public void setActive(boolean active) { 
        this.active = active; 
    }

    public boolean isWindowSwitchingAllowed() { 
        return windowSwitchingAllowed; 
    }
    
    public void setWindowSwitchingAllowed(boolean windowSwitchingAllowed) { 
        this.windowSwitchingAllowed = windowSwitchingAllowed; 
    }

    public Integer getMaxAttempts() { 
        return maxAttempts; 
    }
    
    public void setMaxAttempts(Integer maxAttempts) { 
        this.maxAttempts = maxAttempts; 
    }

    public List<Question> getQuestions() { 
        return questions; 
    }
    
    public void setQuestions(List<Question> questions) { 
        this.questions = questions; 
    }

    public List<ExamAttempt> getExamAttempts() { 
        return examAttempts; 
    }
    
    public void setExamAttempts(List<ExamAttempt> examAttempts) { 
        this.examAttempts = examAttempts; 
    }

    // Helper methods
    public boolean isAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return active && now.isAfter(startTime) && now.isBefore(endTime);
    }

    public boolean isUpcoming() {
        return active && LocalDateTime.now().isBefore(startTime);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }
}