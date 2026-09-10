package com.capacityconnect.controller;

import com.capacityconnect.dto.AssessmentAttemptRequest;
import com.capacityconnect.dto.AssessmentAttemptResponse;
import com.capacityconnect.dto.AssessmentSubmissionRequest;
import com.capacityconnect.entity.User;
import com.capacityconnect.service.AssessmentAttemptService;
import com.capacityconnect.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessment-attempts")
public class AssessmentAttemptController {

    private final AssessmentAttemptService attemptService;
    private final CurrentUserService currentUserService;

    public AssessmentAttemptController(
            AssessmentAttemptService attemptService,
            CurrentUserService currentUserService) {
        this.attemptService = attemptService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AssessmentAttemptResponse> getAllAttempts() {
        return attemptService.getAllAttempts();
    }

    @GetMapping("/{id}")
    public AssessmentAttemptResponse getAttemptById(
            @PathVariable Long id) {
        return attemptService.getAttemptById(id);
    }

    @GetMapping("/assessment/{assessmentId}")
    public List<AssessmentAttemptResponse> getByAssessment(
            @PathVariable Long assessmentId) {
        return attemptService.getByAssessment(assessmentId);
    }

    @GetMapping("/me")
    public List<AssessmentAttemptResponse> getMyAttempts(
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        return attemptService.getByTrainee(currentUser.getId());
    }

    @GetMapping("/trainee/{traineeId}")
    public List<AssessmentAttemptResponse> getByTrainee(
            @PathVariable Long traineeId) {
        return attemptService.getByTrainee(traineeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentAttemptResponse startAttempt(
            @Valid @RequestBody AssessmentAttemptRequest request,
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        return attemptService.startAttempt(
                request,
                currentUser.getId());
    }

    @PatchMapping("/{id}/submit")
    public AssessmentAttemptResponse submitAttempt(
            @PathVariable Long id,
            @Valid @RequestBody AssessmentSubmissionRequest request,
            Authentication authentication) {
        return attemptService.submitAttempt(id, request, authentication);
    }

    @PatchMapping("/{id}/terminate")
    public AssessmentAttemptResponse terminateAttempt(
            @PathVariable Long id,
            Authentication authentication) {
        attemptService.terminateAttempt(id, authentication);
        return attemptService.getAttemptById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttempt(@PathVariable Long id) {
        attemptService.deleteAttempt(id);
    }
}
