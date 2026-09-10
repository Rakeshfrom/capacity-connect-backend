package com.capacityconnect.service;

import com.capacityconnect.dto.AssessmentAttemptRequest;
import com.capacityconnect.dto.AssessmentAttemptResponse;
import com.capacityconnect.entity.Assessment;
import com.capacityconnect.entity.AssessmentAttempt;
import com.capacityconnect.entity.Question;
import com.capacityconnect.dto.AssessmentSubmissionRequest;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.AssessmentAttemptRepository;
import com.capacityconnect.repository.QuestionRepository;
import com.capacityconnect.repository.AssessmentRepository;
import com.capacityconnect.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssessmentAttemptService {

    private final AssessmentAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final AssessmentRepository assessmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUserService;
    private final EnrollmentService enrollmentService;

    public AssessmentAttemptService(
            AssessmentAttemptRepository attemptRepository,
            QuestionRepository questionRepository,
            AssessmentRepository assessmentRepository,
            EnrollmentRepository enrollmentRepository,
            CurrentUserService currentUserService,
            EnrollmentService enrollmentService) {
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.assessmentRepository = assessmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.currentUserService = currentUserService;
        this.enrollmentService = enrollmentService;
    }

    public List<AssessmentAttemptResponse> getAllAttempts() {
        return attemptRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AssessmentAttemptResponse getAttemptById(Long id) {
        return toResponse(findAttempt(id));
    }

    public List<AssessmentAttemptResponse> getByAssessment(
            Long assessmentId) {
        return attemptRepository.findByAssessmentId(assessmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AssessmentAttemptResponse> getByTrainee(
            Long traineeId) {
        return attemptRepository.findByTraineeId(traineeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AssessmentAttemptResponse startAttempt(
            AssessmentAttemptRequest request,
            Long traineeId) {

        Assessment assessment = assessmentRepository.findById(request.getAssessmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assessment not found with id: " + request.getAssessmentId()));

        if (!enrollmentRepository.existsByTraineeIdAndCourseId(
                traineeId, assessment.getCourseId())) {
            throw new IllegalArgumentException(
                    "Trainee is not enrolled in this course");
        }

        if (assessment.getStatus() != Assessment.Status.PUBLISHED) {
            throw new IllegalArgumentException(
                    "Assessment is not available for attempts");
        }

        List<AssessmentAttempt> existingAttempts =
                attemptRepository.findByAssessmentIdAndTraineeId(
                        request.getAssessmentId(),
                        traineeId);

        for (AssessmentAttempt existing : existingAttempts) {
            if (existing.getResult() == AssessmentAttempt.Result.PENDING &&
                    assessment.getTimeLimitMinutes() != null &&
                    LocalDateTime.now().isAfter(
                            existing.getStartedAt()
                                    .plusMinutes(assessment.getTimeLimitMinutes()))) {

                existing.setResult(AssessmentAttempt.Result.FAILED);
                existing.setSubmittedAt(LocalDateTime.now());
                attemptRepository.save(existing);
            }
        }

        return existingAttempts.stream()
                .filter(attempt ->
                        attempt.getResult() == AssessmentAttempt.Result.PENDING)
                .findFirst()
                .map(this::toResponse)
                .orElseGet(() -> {
                    AssessmentAttempt attempt = AssessmentAttempt.builder()
                            .assessmentId(request.getAssessmentId())
                            .traineeId(traineeId)
                            .score(0)
                            .totalMarks(0)
                            .percentage(0)
                            .result(AssessmentAttempt.Result.PENDING)
                            .build();

                    return toResponse(attemptRepository.save(attempt));
                });
    }

    public AssessmentAttemptResponse submitAttempt(
            Long id,
            AssessmentSubmissionRequest request,
            Authentication authentication) {

        AssessmentAttempt attempt = findAttempt(id);
        requireAttemptAccess(attempt, authentication);

        if (attempt.getResult() != AssessmentAttempt.Result.PENDING) {
            throw new IllegalArgumentException(
                    "Assessment attempt has already been submitted");
        }

        Assessment assessment = assessmentRepository.findById(attempt.getAssessmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assessment not found with id: " + attempt.getAssessmentId()));

        if (assessment.getTimeLimitMinutes() != null &&
                LocalDateTime.now().isAfter(
                        attempt.getStartedAt().plusMinutes(assessment.getTimeLimitMinutes()))) {
            throw new IllegalArgumentException("Assessment time limit has expired");
        }

        List<Question> questions =
                questionRepository.findByAssessmentId(attempt.getAssessmentId());

        if (questions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Assessment has no questions");
        }

        java.util.Set<Long> validQuestionIds = questions.stream()
                .map(Question::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (!validQuestionIds.containsAll(request.getAnswers().keySet())) {
            throw new IllegalArgumentException(
                    "Submission contains invalid question IDs");
        }

        for (String answer : request.getAnswers().values()) {
            if (answer == null || !answer.matches("[ABCD]")) {
                throw new IllegalArgumentException(
                        "Submission contains invalid answer options");
            }
        }

        int totalMarks = questions.stream()
                .mapToInt(Question::getMarks)
                .sum();

        int score = 0;

        for (Question question : questions) {
            String answer = request.getAnswers().get(question.getId());

            if (answer != null &&
                    question.getCorrectOption().name().equalsIgnoreCase(answer)) {
                score += question.getMarks();
            }
        }

        int percentage = (score * 100) / totalMarks;

        int passingPercentage = assessment.getPassingPercentage();

        attempt.setScore(score);
        attempt.setTotalMarks(totalMarks);
        attempt.setPercentage(percentage);
        attempt.setResult(
                percentage >= passingPercentage
                        ? AssessmentAttempt.Result.PASSED
                        : AssessmentAttempt.Result.FAILED
        );
        attempt.setSubmittedAt(LocalDateTime.now());

        AssessmentAttempt savedAttempt = attemptRepository.save(attempt);

        if (savedAttempt.getResult() == AssessmentAttempt.Result.PASSED) {
            completeCourseIfAllAssessmentsPassed(
                    savedAttempt.getTraineeId(),
                    assessment.getCourseId(),
                    authentication
            );
        }

        return toResponse(savedAttempt);
    }

    public void terminateAttempt(
            Long id,
            Authentication authentication) {

        AssessmentAttempt attempt = findAttempt(id);
        requireAttemptAccess(attempt, authentication);

        if (attempt.getResult() != AssessmentAttempt.Result.PENDING) {
            throw new IllegalArgumentException(
                    "Assessment attempt has already ended");
        }

        attempt.setResult(AssessmentAttempt.Result.TERMINATED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attemptRepository.save(attempt);
    }

    public void deleteAttempt(Long id) {
        AssessmentAttempt attempt = findAttempt(id);
        attemptRepository.delete(attempt);
    }

    private void completeCourseIfAllAssessmentsPassed(
            Long traineeId,
            Long courseId,
            Authentication authentication) {

        var enrollment = enrollmentRepository
                .findByTraineeIdAndCourseId(traineeId, courseId)
                .orElse(null);

        if (enrollment == null) {
            return;
        }

        List<Assessment> assessments = assessmentRepository.findAll()
                .stream()
                .filter(item -> courseId.equals(item.getCourseId()))
                .filter(item -> item.getStatus() == Assessment.Status.PUBLISHED)
                .toList();

        if (assessments.isEmpty()) {
            return;
        }

        boolean allPassed = assessments.stream().allMatch(item ->
                attemptRepository.findByAssessmentIdAndTraineeId(
                        item.getId(),
                        traineeId
                )
                .stream()
                .anyMatch(attempt ->
                        attempt.getResult() == AssessmentAttempt.Result.PASSED)
        );

        if (allPassed && enrollment.getProgress() < 100) {
            enrollmentService.updateProgress(enrollment.getId(), 100, authentication);
        }
    }

    private void requireAttemptAccess(
            AssessmentAttempt attempt,
            Authentication authentication) {

        var currentUser = currentUserService.getCurrentUser(authentication);

        if (currentUser.getRole() != com.capacityconnect.entity.User.Role.TRAINEE) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only trainees can access assessment attempts");
        }

        if (!currentUser.getId().equals(attempt.getTraineeId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this assessment attempt");
        }

        Assessment assessment = assessmentRepository.findById(attempt.getAssessmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assessment not found with id: " + attempt.getAssessmentId()));

        var enrollment = enrollmentRepository
                .findByTraineeIdAndCourseId(
                        currentUser.getId(),
                        assessment.getCourseId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Trainee is not enrolled in this course"));

        if (enrollment.getStatus() == com.capacityconnect.entity.Enrollment.Status.DROPPED) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Course access is unavailable for this enrollment");
        }
    }

    private AssessmentAttempt findAttempt(Long id) {
        return attemptRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assessment attempt not found with id: " + id));
    }

    private AssessmentAttemptResponse toResponse(
            AssessmentAttempt attempt) {

        return AssessmentAttemptResponse.builder()
                .id(attempt.getId())
                .assessmentId(attempt.getAssessmentId())
                .traineeId(attempt.getTraineeId())
                .score(attempt.getScore())
                .totalMarks(attempt.getTotalMarks())
                .percentage(attempt.getPercentage())
                .result(attempt.getResult())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }
}
