package com.capacityconnect.controller;

import com.capacityconnect.dto.EnrollmentRequest;
import com.capacityconnect.dto.EnrollmentResponse;
import com.capacityconnect.entity.Enrollment;
import com.capacityconnect.entity.User;
import com.capacityconnect.service.CurrentUserService;
import com.capacityconnect.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final CurrentUserService currentUserService;

    public EnrollmentController(
            EnrollmentService enrollmentService,
            CurrentUserService currentUserService) {
        this.enrollmentService = enrollmentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<EnrollmentResponse> getAllEnrollments() {
        return enrollmentService.getAllEnrollments();
    }

    @GetMapping("/{id}")
    public EnrollmentResponse getEnrollmentById(@PathVariable Long id) {
        return enrollmentService.getEnrollmentById(id);
    }

    @GetMapping("/me")
    public List<EnrollmentResponse> getMyEnrollments(
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        return enrollmentService.getByTrainee(currentUser.getId());
    }

    @GetMapping("/trainee/{traineeId}")
    public List<EnrollmentResponse> getByTrainee(
            @PathVariable Long traineeId) {
        return enrollmentService.getByTrainee(traineeId);
    }

    @GetMapping("/course/{courseId}")
    public List<EnrollmentResponse> getByCourse(
            @PathVariable Long courseId) {
        return enrollmentService.getByCourse(courseId);
    }

    @GetMapping("/status/{status}")
    public List<EnrollmentResponse> getByStatus(
            @PathVariable Enrollment.Status status) {
        return enrollmentService.getByStatus(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse createEnrollment(
            @Valid @RequestBody EnrollmentRequest request) {
        return enrollmentService.createEnrollment(request);
    }

    @PatchMapping("/{id}/progress")
    public EnrollmentResponse updateProgress(
            @PathVariable Long id,
            @RequestParam Integer progress) {
        return enrollmentService.updateProgress(id, progress);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEnrollment(@PathVariable Long id) {
        enrollmentService.deleteEnrollment(id);
    }
}
