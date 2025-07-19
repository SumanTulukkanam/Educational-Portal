package com.example.demo.repository;

import com.example.demo.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByActiveTrue();

    @Query("SELECT e FROM Exam e WHERE e.active = true AND e.startTime <= :now AND e.endTime >= :now")
    List<Exam> findAvailableExams(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Exam e WHERE e.active = true AND e.startTime > :now")
    List<Exam> findUpcomingExams(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Exam e WHERE e.active = true AND e.endTime < :now")
    List<Exam> findExpiredExams(@Param("now") LocalDateTime now);

    List<Exam> findByActiveTrueOrderByStartTimeAsc();
}
