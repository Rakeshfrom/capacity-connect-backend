package com.capacityconnect.controller;

import com.capacityconnect.dto.FeedbackRequest;
import com.capacityconnect.dto.FeedbackResponse;
import com.capacityconnect.entity.User;
import com.capacityconnect.service.CurrentUserService;
import com.capacityconnect.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CurrentUserService currentUserService;

    public FeedbackController(
            FeedbackService feedbackService,
            CurrentUserService currentUserService) {
        this.feedbackService = feedbackService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<FeedbackResponse> getAllFeedback() {
        return feedbackService.getAllFeedback();
    }

    @GetMapping("/{id}")
    public FeedbackResponse getFeedbackById(
            @PathVariable Long id) {
        return feedbackService.getFeedbackById(id);
    }

    @GetMapping("/me")
    public List<FeedbackResponse> getMyFeedback(
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        return feedbackService.getByTrainee(currentUser.getId());
    }

    @GetMapping("/trainee/{traineeId}")
    public List<FeedbackResponse> getByTrainee(
            @PathVariable Long traineeId) {
        return feedbackService.getByTrainee(traineeId);
    }

    @GetMapping("/course/{courseId}")
    public List<FeedbackResponse> getByCourse(
            @PathVariable Long courseId) {
        return feedbackService.getByCourse(courseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse createFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        return feedbackService.createFeedback(
                request,
                currentUser.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
    }
}
