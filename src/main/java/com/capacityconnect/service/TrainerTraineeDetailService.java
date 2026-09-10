package com.capacityconnect.service;

import com.capacityconnect.dto.TrainerTraineeDetailResponse;
import com.capacityconnect.entity.Assessment;
import com.capacityconnect.entity.AssessmentAttempt;
import com.capacityconnect.entity.Course;
import com.capacityconnect.entity.Enrollment;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.AssessmentAttemptRepository;
import com.capacityconnect.repository.AssessmentRepository;
import com.capacityconnect.repository.CourseRepository;
import com.capacityconnect.repository.EnrollmentRepository;
import com.capacityconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainerTraineeDetailService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;

    public TrainerTraineeDetailResponse getTraineeDetail(
            Long trainerId,
            Long traineeId
    ) {
        User trainee = userRepository.findById(traineeId)
                .orElseThrow(() -> new IllegalArgumentException("Trainee not found"));

        List<Course> trainerCourses = courseRepository.findByTrainerId(trainerId);
        List<Enrollment> enrollments = enrollmentRepository.findByTraineeId(traineeId);

        List<TrainerTraineeDetailResponse.CoursePerformance> courses =
                trainerCourses.stream()
                        .filter(course -> enrollments.stream()
                                .anyMatch(enrollment ->
                                        enrollment.getCourseId().equals(course.getId())))
                        .map(course -> buildCoursePerformance(course, enrollments, traineeId))
                        .toList();

        return TrainerTraineeDetailResponse.builder()
                .traineeId(trainee.getId())
                .username(trainee.getUsername())
                .firstName(trainee.getFirstName())
                .lastName(trainee.getLastName())
                .email(trainee.getEmail())
                .courses(courses)
                .build();
    }

    private TrainerTraineeDetailResponse.CoursePerformance buildCoursePerformance(
            Course course,
            List<Enrollment> enrollments,
            Long traineeId
    ) {
        Enrollment enrollment = enrollments.stream()
                .filter(item -> item.getCourseId().equals(course.getId()))
                .findFirst()
                .orElseThrow();

        List<Assessment> assessments =
                assessmentRepository.findByCourseId(course.getId());

        List<TrainerTraineeDetailResponse.AssessmentPerformance> performance =
                assessments.stream()
                        .map(assessment -> buildAssessmentPerformance(
                                assessment,
                                traineeId
                        ))
                        .toList();

        return TrainerTraineeDetailResponse.CoursePerformance.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .enrollmentStatus(enrollment.getStatus().name())
                .progress(enrollment.getProgress())
                .assessments(performance)
                .build();
    }

    private TrainerTraineeDetailResponse.AssessmentPerformance buildAssessmentPerformance(
            Assessment assessment,
            Long traineeId
    ) {
        List<AssessmentAttempt> attempts =
                assessmentAttemptRepository
                        .findByAssessmentIdAndTraineeId(
                                assessment.getId(),
                                traineeId
                        );

        AssessmentAttempt latestCompleted = attempts.stream()
                .filter(attempt -> attempt.getResult() != AssessmentAttempt.Result.PENDING)
                .max(Comparator.comparing(
                        AssessmentAttempt::getSubmittedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ))
                .orElse(null);

        return TrainerTraineeDetailResponse.AssessmentPerformance.builder()
                .assessmentId(assessment.getId())
                .assessmentTitle(assessment.getTitle())
                .percentage(
                        latestCompleted != null
                                ? latestCompleted.getPercentage()
                                : null
                )
                .result(
                        latestCompleted != null
                                ? latestCompleted.getResult().name()
                                : "NOT_ATTEMPTED"
                )
                .build();
    }
}
