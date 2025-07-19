package com.example.demo.repository;

import com.example.demo.model.Answer;
import com.example.demo.model.ExamAttempt;
import com.example.demo.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    
    List<Answer> findByExamAttempt(ExamAttempt examAttempt);
    
    Optional<Answer> findByExamAttemptAndQuestion(ExamAttempt examAttempt, Question question);
    
    List<Answer> findByExamAttemptOrderByQuestionQuestionNumberAsc(ExamAttempt examAttempt);
    
    long countByExamAttemptAndIsCorrectTrue(ExamAttempt examAttempt);
    
    void deleteByExamAttempt(ExamAttempt examAttempt);
}