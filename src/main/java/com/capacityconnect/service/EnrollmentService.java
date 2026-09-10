package com.capacityconnect.service;

import com.capacityconnect.dto.EnrollmentRequest;
import com.capacityconnect.dto.EnrollmentResponse;
import com.capacityconnect.entity.Enrollment;
import com.capacityconnect.exception.DuplicateResourceException;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<EnrollmentResponse> getAllEnrollments() {
        return enrollmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EnrollmentResponse getEnrollmentById(Long id) {
        return toResponse(findEnrollment(id));
    }

    public List<EnrollmentResponse> getByTrainee(Long traineeId) {
        return enrollmentRepository.findByTraineeId(traineeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<EnrollmentResponse> getByCourse(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<EnrollmentResponse> getByStatus(Enrollment.Status status) {
        return enrollmentRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EnrollmentResponse createEnrollment(EnrollmentRequest request) {
        if (enrollmentRepository.existsByTraineeIdAndCourseId(
                request.getTraineeId(), request.getCourseId())) {
            throw new DuplicateResourceException(
                    "Trainee is already enrolled in this course");
        }

        Enrollment enrollment = Enrollment.builder()
                .traineeId(request.getTraineeId())
                .courseId(request.getCourseId())
                .status(Enrollment.Status.ENROLLED)
                .progress(0)
                .build();

        return toResponse(enrollmentRepository.save(enrollment));
    }

    public EnrollmentResponse updateProgress(Long id, Integer progress) {
        Enrollment enrollment = findEnrollment(id);

        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException(
                    "Progress must be between 0 and 100");
        }

        enrollment.setProgress(progress);

        if (progress == 100) {
            enrollment.setStatus(Enrollment.Status.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
        } else if (progress > 0) {
            enrollment.setStatus(Enrollment.Status.IN_PROGRESS);
            enrollment.setCompletedAt(null);
        }

        return toResponse(enrollmentRepository.save(enrollment));
    }

    public void deleteEnrollment(Long id) {
        Enrollment enrollment = findEnrollment(id);
        enrollmentRepository.delete(enrollment);
    }

    private Enrollment findEnrollment(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Enrollment not found with id: " + id));
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .traineeId(enrollment.getTraineeId())
                .courseId(enrollment.getCourseId())
                .status(enrollment.getStatus())
                .progress(enrollment.getProgress())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .build();
    }
}
