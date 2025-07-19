package com.example.demo.repository;
import com.example.demo.model.ExamAttempt;
import com.example.demo.model.Student;
import com.example.demo.model.Exam;
import com.example.demo.model.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    List<ExamAttempt> findByStudent(Student student);

    List<ExamAttempt> findByExam(Exam exam);

    List<ExamAttempt> findByStudentAndExam(Student student, Exam exam);

    Optional<ExamAttempt> findByStudentAndExamAndStatus(Student student, Exam exam, AttemptStatus status);

    long countByStudentAndExam(Student student, Exam exam);

    List<ExamAttempt> findByStudentOrderByStartTimeDesc(Student student);

    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.status = 'IN_PROGRESS' AND ea.timeRemaining <= 0")
    List<ExamAttempt> findTimedOutAttempts();

    @Query("SELECT COUNT(ea) FROM ExamAttempt ea WHERE ea.exam = :exam AND ea.status = 'COMPLETED'")
    long countCompletedAttemptsByExam(Exam exam);

    @Query("SELECT AVG(ea.obtainedMarks) FROM ExamAttempt ea WHERE ea.exam = :exam AND ea.status = 'COMPLETED'")
    Double getAverageScoreByExam(Exam exam);
}