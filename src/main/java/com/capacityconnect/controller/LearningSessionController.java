package com.capacityconnect.controller;

import com.capacityconnect.dto.LearningSessionStartRequest;
import com.capacityconnect.entity.LearningSession;
import com.capacityconnect.entity.User;
import com.capacityconnect.service.CurrentUserService;
import com.capacityconnect.service.LearningSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/learning-sessions")
public class LearningSessionController {

    private final LearningSessionService learningSessionService;
    private final CurrentUserService currentUserService;

    public LearningSessionController(
            LearningSessionService learningSessionService,
            CurrentUserService currentUserService
    ) {
        this.learningSessionService = learningSessionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningSession start(
            @RequestBody LearningSessionStartRequest request,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);

        if (user.getRole() != User.Role.TRAINEE) {
            throw new IllegalStateException("Only trainees can start learning sessions");
        }

        return learningSessionService.start(user.getId(), request);
    }

    @PostMapping("/{id}/heartbeat")
    public LearningSession heartbeat(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        return learningSessionService.heartbeat(user.getId(), id);
    }

    @PostMapping("/{id}/stop")
    public LearningSession stop(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        return learningSessionService.stop(user.getId(), id);
    }

    @GetMapping("/me/summary")
    public Map<String, Object> getMySummary(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);

        if (user.getRole() != User.Role.TRAINEE) {
            throw new IllegalStateException("Only trainees can access this learning summary");
        }

        return learningSessionService.getMySummary(user.getId());
    }
}
