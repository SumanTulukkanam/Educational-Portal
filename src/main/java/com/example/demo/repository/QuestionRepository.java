
// QuestionRepository.java
package com.example.demo.repository;
import com.example.demo.model.Question;
import com.example.demo.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByExamOrderByQuestionNumberAsc(Exam exam);

    List<Question> findByExamIdOrderByQuestionNumberAsc(Long examId);

    long countByExam(Exam exam);

    void deleteByExam(Exam exam);
}