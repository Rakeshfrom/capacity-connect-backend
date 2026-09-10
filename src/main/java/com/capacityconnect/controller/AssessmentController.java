package com.capacityconnect.controller;

import com.capacityconnect.dto.AssessmentRequest;
import com.capacityconnect.dto.AssessmentResponse;
import com.capacityconnect.entity.Assessment;
import com.capacityconnect.service.AssessmentService;
import com.capacityconnect.service.EnrollmentAccessService;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final EnrollmentAccessService enrollmentAccessService;

    public AssessmentController(
            AssessmentService assessmentService,
            EnrollmentAccessService enrollmentAccessService) {
        this.assessmentService = assessmentService;
        this.enrollmentAccessService = enrollmentAccessService;
    }

    @GetMapping
    public List<AssessmentResponse> getAllAssessments() {
        return assessmentService.getAllAssessments();
    }

    @GetMapping("/{id}")
    public AssessmentResponse getAssessmentById(@PathVariable Long id) {
        return assessmentService.getAssessmentById(id);
    }

    @GetMapping("/course/{courseId}")
    public List<AssessmentResponse> getByCourse(
            @PathVariable Long courseId) {
        return assessmentService.getByCourse(courseId);
    }

    @GetMapping("/trainee/course/{courseId}")
    public List<AssessmentResponse> getForEnrolledTrainee(
            @PathVariable Long courseId,
            Authentication authentication) {

        enrollmentAccessService.requireEnrolledTrainee(
                courseId, authentication);

        return assessmentService.getByCourse(courseId);
    }

    @GetMapping("/status/{status}")
    public List<AssessmentResponse> getByStatus(
            @PathVariable Assessment.Status status) {
        return assessmentService.getByStatus(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentResponse createAssessment(
            @Valid @RequestBody AssessmentRequest request) {
        return assessmentService.createAssessment(request);
    }

    @PutMapping("/{id}")
    public AssessmentResponse updateAssessment(
            @PathVariable Long id,
            @Valid @RequestBody AssessmentRequest request) {
        return assessmentService.updateAssessment(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssessment(@PathVariable Long id) {
        assessmentService.deleteAssessment(id);
    }
}
