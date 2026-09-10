package com.capacityconnect.service;

import com.capacityconnect.dto.TrainerAnalyticsResponse;
import com.capacityconnect.entity.Assessment;
import com.capacityconnect.entity.AssessmentAttempt;
import com.capacityconnect.entity.Course;
import com.capacityconnect.entity.Enrollment;
import com.capacityconnect.repository.AssessmentAttemptRepository;
import com.capacityconnect.repository.AssessmentRepository;
import com.capacityconnect.repository.CourseRepository;
import com.capacityconnect.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrainerAnalyticsService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;

    public TrainerAnalyticsResponse getAnalytics(Long trainerId) {
        List<Course> courses = courseRepository.findByTrainerId(trainerId);

        Set<Long> traineeIds = new HashSet<>();
        int totalProgress = 0;
        int enrollmentCount = 0;

        int totalScore = 0;
        int scoreCount = 0;

        for (Course course : courses) {
            List<Enrollment> enrollments =
                    enrollmentRepository.findByCourseId(course.getId());

            for (Enrollment enrollment : enrollments) {
                traineeIds.add(enrollment.getTraineeId());
                totalProgress += enrollment.getProgress();
                enrollmentCount++;
            }

            List<Assessment> assessments =
                    assessmentRepository.findByCourseId(course.getId());

            for (Assessment assessment : assessments) {
                List<AssessmentAttempt> attempts =
                        assessmentAttemptRepository.findByAssessmentId(
                                assessment.getId()
                        );

                AssessmentAttempt latestCompleted = attempts.stream()
                        .filter(attempt ->
                                attempt.getResult() != AssessmentAttempt.Result.PENDING)
                        .max(Comparator.comparing(
                                AssessmentAttempt::getSubmittedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())
                        ))
                        .orElse(null);

                if (latestCompleted != null) {
                    totalScore += latestCompleted.getPercentage();
                    scoreCount++;
                }
            }
        }

        int averageCompletion =
                enrollmentCount == 0 ? 0 : totalProgress / enrollmentCount;

        int averageAssessmentScore =
                scoreCount == 0 ? 0 : totalScore / scoreCount;

        int overallPerformance =
                (averageCompletion + averageAssessmentScore) / 2;

        List<TrainerAnalyticsResponse.CourseAnalytics> courseAnalytics =
                courses.stream()
                        .map(this::buildCourseAnalytics)
                        .toList();

        return TrainerAnalyticsResponse.builder()
                .trainerId(trainerId)
                .totalTrainees(traineeIds.size())
                .averageCompletion(averageCompletion)
                .averageAssessmentScore(averageAssessmentScore)
                .overallPerformance(overallPerformance)
                .courses(courseAnalytics)
                .build();
    }

    private TrainerAnalyticsResponse.CourseAnalytics buildCourseAnalytics(
            Course course
    ) {
        List<Enrollment> enrollments =
                enrollmentRepository.findByCourseId(course.getId());

        int completionTotal = 0;

        for (Enrollment enrollment : enrollments) {
            completionTotal += enrollment.getProgress();
        }

        int completion =
                enrollments.isEmpty()
                        ? 0
                        : completionTotal / enrollments.size();

        List<Assessment> assessments =
                assessmentRepository.findByCourseId(course.getId());

        int scoreTotal = 0;
        int scoreCount = 0;

        for (Assessment assessment : assessments) {
            List<AssessmentAttempt> attempts =
                    assessmentAttemptRepository.findByAssessmentId(
                            assessment.getId()
                    );

            AssessmentAttempt latestCompleted = attempts.stream()
                    .filter(attempt ->
                            attempt.getResult() != AssessmentAttempt.Result.PENDING)
                    .max(Comparator.comparing(
                            AssessmentAttempt::getSubmittedAt,
                            Comparator.nullsFirst(Comparator.naturalOrder())
                    ))
                    .orElse(null);

            if (latestCompleted != null) {
                scoreTotal += latestCompleted.getPercentage();
                scoreCount++;
            }
        }

        int averageScore =
                scoreCount == 0 ? 0 : scoreTotal / scoreCount;

        return TrainerAnalyticsResponse.CourseAnalytics.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .trainees(enrollments.size())
                .completion(completion)
                .averageScore(averageScore)
                .build();
    }
}
