package com.capacityconnect.service;

import com.capacityconnect.dto.AdminAnalyticsResponse;
import com.capacityconnect.entity.Assessment;
import com.capacityconnect.entity.AssessmentAttempt;
import com.capacityconnect.entity.Course;
import com.capacityconnect.entity.Enrollment;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.AssessmentAttemptRepository;
import com.capacityconnect.repository.AssessmentRepository;
import com.capacityconnect.repository.CourseRepository;
import com.capacityconnect.repository.EnrollmentRepository;
import com.capacityconnect.repository.FeedbackRepository;
import com.capacityconnect.repository.TrainerResourceRepository;
import com.capacityconnect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final FeedbackRepository feedbackRepository;
    private final TrainerResourceRepository trainerResourceRepository;

    public AdminAnalyticsService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssessmentRepository assessmentRepository,
            AssessmentAttemptRepository assessmentAttemptRepository,
            FeedbackRepository feedbackRepository,
            TrainerResourceRepository trainerResourceRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
        this.feedbackRepository = feedbackRepository;
        this.trainerResourceRepository = trainerResourceRepository;
    }

    public AdminAnalyticsResponse getAnalytics() {
        long totalTrainees = userRepository.countByRoleAndStatus(
                User.Role.TRAINEE,
                User.Status.ACTIVE
        );

        List<Course> courses = courseRepository.findAll();

        long activeCourses = courses.stream()
                .filter(course -> course.getStatus() == Course.Status.PUBLISHED)
                .count();

        Double averageProgress = enrollmentRepository.findAverageProgress();
        Double averageScore = assessmentAttemptRepository.findAveragePercentage();

        int overallCompletion = averageProgress == null
                ? 0
                : (int) Math.round(averageProgress);

        int averageAssessmentScore = averageScore == null
                ? 0
                : (int) Math.round(averageScore);

        long totalEnrollments = enrollmentRepository.count();

        int courseParticipation = totalTrainees == 0
                ? 0
                : (int) Math.min(
                        100,
                        Math.round((double) totalEnrollments / totalTrainees * 100)
                );

        long submittedAttempts =
                assessmentAttemptRepository.countSubmittedAttempts();

        int assessmentParticipation = totalTrainees == 0
                ? 0
                : (int) Math.min(
                        100,
                        Math.round((double) submittedAttempts / totalTrainees * 100)
                );

        long feedbackCount = feedbackRepository.count();

        int feedbackResponse = totalEnrollments == 0
                ? 0
                : (int) Math.min(
                        100,
                        Math.round((double) feedbackCount / totalEnrollments * 100)
                );

        long resourceCount = trainerResourceRepository.count();

        int resourceEngagement = activeCourses == 0
                ? 0
                : (int) Math.min(
                        100,
                        Math.round((double) resourceCount / activeCourses * 100)
                );

        List<AdminAnalyticsResponse.CourseAnalytics> courseAnalytics =
                courses.stream()
                        .filter(course -> course.getStatus() == Course.Status.PUBLISHED)
                        .map(this::buildCourseAnalytics)
                        .toList();

        return AdminAnalyticsResponse.builder()
                .totalTrainees(totalTrainees)
                .activeCourses(activeCourses)
                .averageAssessmentScore(averageAssessmentScore)
                .overallCompletion(overallCompletion)
                .courseParticipation(courseParticipation)
                .assessmentParticipation(assessmentParticipation)
                .resourceEngagement(resourceEngagement)
                .feedbackResponse(feedbackResponse)
                .courses(courseAnalytics)
                .build();
    }

    private AdminAnalyticsResponse.CourseAnalytics buildCourseAnalytics(
            Course course
    ) {
        List<Enrollment> enrollments =
                enrollmentRepository.findByCourseId(course.getId());

        int completion = enrollments.isEmpty()
                ? 0
                : (int) Math.round(
                        enrollments.stream()
                                .mapToInt(e -> e.getProgress() == null
                                        ? 0
                                        : e.getProgress())
                                .average()
                                .orElse(0)
                );

        List<Assessment> assessments =
                assessmentRepository.findByCourseId(course.getId());

        List<AssessmentAttempt> attempts = assessments.stream()
                .flatMap(assessment ->
                        assessmentAttemptRepository
                                .findByAssessmentId(assessment.getId())
                                .stream())
                .filter(attempt -> attempt.getPercentage() != null)
                .filter(attempt ->
                        attempt.getResult() == AssessmentAttempt.Result.PASSED
                                || attempt.getResult() == AssessmentAttempt.Result.FAILED)
                .toList();

        int averageScore = attempts.isEmpty()
                ? 0
                : (int) Math.round(
                        attempts.stream()
                                .mapToInt(AssessmentAttempt::getPercentage)
                                .average()
                                .orElse(0)
                );

        return AdminAnalyticsResponse.CourseAnalytics.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .enrolled((long) enrollments.size())
                .completion(completion)
                .averageScore(averageScore)
                .build();
    }
}
