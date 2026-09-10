package com.capacityconnect.controller;

import com.capacityconnect.dto.QuestionnaireResponseDto;
import com.capacityconnect.dto.QuestionnaireResponseRequest;
import com.capacityconnect.entity.User;
import com.capacityconnect.service.CurrentUserService;
import com.capacityconnect.service.QuestionnaireResponseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questionnaire-responses")
public class QuestionnaireResponseController {

    private final QuestionnaireResponseService service;
    private final CurrentUserService currentUserService;

    public QuestionnaireResponseController(
            QuestionnaireResponseService service,
            CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<QuestionnaireResponseDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public QuestionnaireResponseDto getById(
            @PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/questionnaire/{questionnaireId}")
    public List<QuestionnaireResponseDto> getByQuestionnaire(
            @PathVariable Long questionnaireId) {
        return service.getByQuestionnaire(questionnaireId);
    }

    @GetMapping("/me")
    public List<QuestionnaireResponseDto> getMyResponses(
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        return service.getByTrainee(currentUser.getId());
    }

    @GetMapping("/trainee/{traineeId}")
    public List<QuestionnaireResponseDto> getByTrainee(
            @PathVariable Long traineeId) {
        return service.getByTrainee(traineeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionnaireResponseDto create(
            @Valid @RequestBody QuestionnaireResponseRequest request,
            Authentication authentication) {

        User currentUser = currentUserService.getCurrentUser(authentication);

        return service.create(request, currentUser.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
