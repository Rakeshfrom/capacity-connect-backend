package com.capacityconnect.service;

import com.capacityconnect.entity.Enrollment;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.EnrollmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentAccessService {

    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUserService;

    public EnrollmentAccessService(
            EnrollmentRepository enrollmentRepository,
            CurrentUserService currentUserService) {
        this.enrollmentRepository = enrollmentRepository;
        this.currentUserService = currentUserService;
    }

    public User requireEnrolledTrainee(
            Long courseId,
            Authentication authentication) {

        User user = currentUserService.getCurrentUser(authentication);

        if (user.getRole() != User.Role.TRAINEE) {
            throw new IllegalStateException(
                    "Only trainees can access enrolled course content");
        }

        Enrollment enrollment = enrollmentRepository
                .findByTraineeIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new IllegalStateException(
                        "Trainee is not enrolled in this course"));

        if (enrollment.getStatus() == Enrollment.Status.DROPPED) {
            throw new IllegalStateException(
                    "Course access is unavailable for this enrollment");
        }

        return user;
    }
}
