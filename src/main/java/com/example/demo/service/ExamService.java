package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.model.Answer;
import com.example.demo.model.AttemptStatus;
import com.example.demo.model.Exam;
import com.example.demo.model.ExamAttempt;
import com.example.demo.model.Question;
import com.example.demo.model.Student;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.ExamAttemptRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.QuestionRepository;

@Service
@Transactional
public class ExamService {

    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ExamAttemptRepository examAttemptRepository;
    
    @Autowired
    private AnswerRepository answerRepository;

    // Exam Management
    public List<Exam> getAllActiveExams() {
        return examRepository.findByActiveTrue();
    }

    public Optional<Exam> getExamById(Long id) {
        return examRepository.findById(id);
    }

    public Exam saveExam(Exam exam) {
        return examRepository.save(exam);
    }

    public List<ExamAttempt> getExamAttemptsByExam(Long examId) {
        Optional<Exam> examOpt = examRepository.findById(examId);
        if (examOpt.isPresent()) {
            return examAttemptRepository.findByExam(examOpt.get());
        }
        return new ArrayList<>();
    }

    public void deleteExam(Long examId) {
        Optional<Exam> examOpt = examRepository.findById(examId);
        if (examOpt.isPresent()) {
            Exam exam = examOpt.get();
            // Delete all related data
            List<ExamAttempt> attempts = examAttemptRepository.findByExam(exam);
            for (ExamAttempt attempt : attempts) {
                answerRepository.deleteByExamAttempt(attempt);
            }
            examAttemptRepository.deleteAll(attempts);
            questionRepository.deleteByExam(exam);
            examRepository.delete(exam);
        }
    }

    // Question Management
    public List<Question> getQuestionsByExam(Long examId) {
        return questionRepository.findByExamIdOrderByQuestionNumberAsc(examId);
    }

    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }

    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }

    // Exam Availability
    public List<Exam> getAvailableExams() {
        return examRepository.findAvailableExams(LocalDateTime.now());
    }

    public List<Exam> getUpcomingExams() {
        return examRepository.findUpcomingExams(LocalDateTime.now());
    }

    // Student Exam Attempts
    public boolean canStudentAttemptExam(Student student, Exam exam) {
        if (!exam.isAvailable()) {
            return false;
        }

        long attemptCount = examAttemptRepository.countByStudentAndExam(student, exam);
        return attemptCount < exam.getMaxAttempts();
    }

    public ExamAttempt startExam(Student student, Exam exam) {
        if (!canStudentAttemptExam(student, exam)) {
            throw new RuntimeException("Cannot start exam");
        }
        
        ExamAttempt attempt = new ExamAttempt();
        attempt.setStudent(student);
        attempt.setExam(exam);
        attempt.setStartTime(LocalDateTime.now());
        attempt.setTimeRemaining(exam.getDuration() * 60); // Convert minutes to seconds
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setTotalQuestions(questionRepository.findByExamIdOrderByQuestionNumberAsc(exam.getId()).size());

        return examAttemptRepository.save(attempt);
    }

    public ExamAttempt saveAnswer(ExamAttempt attempt, Long questionId, String selectedAnswer) {
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (questionOpt.isEmpty()) {
            throw new RuntimeException("Question not found");
        }
        Question question = questionOpt.get();

        // Check if answer already exists
        Optional<Answer> existingAnswer = answerRepository.findByExamAttemptAndQuestion(attempt, question);

        Answer answer;
        if (existingAnswer.isPresent()) {
            answer = existingAnswer.get();
            answer.setSelectedAnswer(selectedAnswer);
        } else {
            answer = new Answer(attempt, question, selectedAnswer);
        }

        answerRepository.save(answer);
        return attempt;
    }

    public ExamAttempt submitExam(ExamAttempt attempt) {
        attempt.setEndTime(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.COMPLETED);

        // Calculate results
        calculateExamResults(attempt);

        return examAttemptRepository.save(attempt);
    }

    public ExamAttempt recordWindowSwitch(ExamAttempt attempt) {
        attempt.setWindowSwitchCount(attempt.getWindowSwitchCount() + 1);

        // Terminate exam if too many window switches (more than 3)
        if (attempt.getWindowSwitchCount() > 3) {
            attempt.setStatus(AttemptStatus.TERMINATED);
            attempt.setEndTime(LocalDateTime.now());
            attempt.setRemarks("Terminated due to excessive window switching");
            calculateExamResults(attempt);
        }

        return examAttemptRepository.save(attempt);
    }

    public ExamAttempt updateTimeRemaining(ExamAttempt attempt, Integer timeRemaining) {
        attempt.setTimeRemaining(timeRemaining);

        if (timeRemaining <= 0 && attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            attempt.setStatus(AttemptStatus.TIMED_OUT);
            attempt.setEndTime(LocalDateTime.now());
            attempt.setAutoSubmitted(true);
            calculateExamResults(attempt);
        }

        return examAttemptRepository.save(attempt);
    }

    private void calculateExamResults(ExamAttempt attempt) {
        List<Answer> answers = answerRepository.findByExamAttempt(attempt);

        int correctAnswers = 0;
        int wrongAnswers = 0;
        int obtainedMarks = 0;

        for (Answer answer : answers) {
            if (answer.isCorrect()) {
                correctAnswers++;
                obtainedMarks += answer.getMarksObtained();
            } else if (answer.getSelectedAnswer() != null && !answer.getSelectedAnswer().trim().isEmpty()) {
                wrongAnswers++;
            }
        }

        attempt.setCorrectAnswers(correctAnswers);
        attempt.setWrongAnswers(wrongAnswers);
        attempt.setUnansweredQuestions(attempt.getTotalQuestions() - correctAnswers - wrongAnswers);
        attempt.setObtainedMarks(obtainedMarks);
    }

    // Result Management
    public List<ExamAttempt> getExamAttempts(Student student) {
        return examAttemptRepository.findByStudent(student);
    }

    public Optional<ExamAttempt> getAttemptById(Long attemptId) {
        return examAttemptRepository.findById(attemptId);
    }

    public List<Answer> getAttemptAnswers(ExamAttempt attempt) {
        return answerRepository.findByExamAttemptOrderByQuestionQuestionNumberAsc(attempt);
    }

    public List<ExamAttempt> getExamAttempts(Exam exam) {
        return examAttemptRepository.findByExam(exam);
    }

    public long getCompletedAttemptsCount(Exam exam) {
        return examAttemptRepository.countCompletedAttemptsByExam(exam);
    }

    public Double getAverageScore(Exam exam) {
        Double avg = examAttemptRepository.getAverageScoreByExam(exam);
        return avg != null ? avg : 0.0;
    }
}