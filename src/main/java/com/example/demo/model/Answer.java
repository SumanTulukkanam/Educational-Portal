package com.example.demo.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_attempt_id", nullable = false)
    private ExamAttempt examAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column
    private String selectedAnswer; // A, B, C, D or text answer

    @Column(nullable = false)
    private boolean isCorrect = false;

    @Column(nullable = false)
    private Integer marksObtained = 0;

    @Column(nullable = false)
    private LocalDateTime answeredAt;

    // Constructors
    public Answer() {}

    public Answer(ExamAttempt examAttempt, Question question, String selectedAnswer) {
        this.examAttempt = examAttempt;
        this.question = question;
        this.selectedAnswer = selectedAnswer;
        this.answeredAt = LocalDateTime.now();

        // Add null check and proper comparison
        if (selectedAnswer != null && !selectedAnswer.trim().isEmpty() &&
            question != null && question.getCorrectAnswer() != null) {
            this.isCorrect = selectedAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
            this.marksObtained = this.isCorrect ? (question.getMarks() != null ? question.getMarks() : 0) : 0;
        } else {
            this.isCorrect = false;
            this.marksObtained = 0;
        }
    }

    // Getters and Setters
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }

    public ExamAttempt getExamAttempt() { 
        return examAttempt; 
    }
    
    public void setExamAttempt(ExamAttempt examAttempt) { 
        this.examAttempt = examAttempt;
    }

    public Question getQuestion() { 
        return question; 
    }
    
    public void setQuestion(Question question) { 
        this.question = question; 
    }

    public String getSelectedAnswer() { 
        return selectedAnswer; 
    }
    
    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
        if (selectedAnswer != null && !selectedAnswer.trim().isEmpty() &&
            this.question != null && this.question.getCorrectAnswer() != null) {
            this.isCorrect = selectedAnswer.trim().equalsIgnoreCase(this.question.getCorrectAnswer().trim());
            this.marksObtained = this.isCorrect ? (this.question.getMarks() != null ? this.question.getMarks() : 0) : 0;
        } else {
            this.isCorrect = false;
            this.marksObtained = 0;
        }
    }

    public boolean isCorrect() { 
        return isCorrect; 
    }
    
    public void setCorrect(boolean correct) { 
        isCorrect = correct; 
    }

    public Integer getMarksObtained() { 
        return marksObtained; 
    }
    
    public void setMarksObtained(Integer marksObtained) { 
        this.marksObtained = marksObtained; 
    }

    public LocalDateTime getAnsweredAt() { 
        return answeredAt; 
    }
    
    public void setAnsweredAt(LocalDateTime answeredAt) { 
        this.answeredAt = answeredAt; 
    }
}
