package com.capacityconnect.controller;

import com.capacityconnect.dto.QuestionnaireRequest;
import com.capacityconnect.dto.QuestionnaireResponse;
import com.capacityconnect.service.QuestionnaireService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questionnaires")
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    public QuestionnaireController(QuestionnaireService questionnaireService) {
        this.questionnaireService = questionnaireService;
    }

    @GetMapping
    public List<QuestionnaireResponse> getAll() {
        return questionnaireService.getAll();
    }

    @GetMapping("/{id}")
    public QuestionnaireResponse getById(@PathVariable Long id) {
        return questionnaireService.getById(id);
    }

    @GetMapping("/trainer/{trainerId}")
    public List<QuestionnaireResponse> getByTrainer(
            @PathVariable Long trainerId) {
        return questionnaireService.getByTrainer(trainerId);
    }

    @GetMapping("/course/{courseId}")
    public List<QuestionnaireResponse> getByCourse(
            @PathVariable Long courseId) {
        return questionnaireService.getByCourse(courseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionnaireResponse create(
            @Valid @RequestBody QuestionnaireRequest request) {
        return questionnaireService.create(request);
    }

    @PutMapping("/{id}")
    public QuestionnaireResponse update(
            @PathVariable Long id,
            @Valid @RequestBody QuestionnaireRequest request) {
        return questionnaireService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        questionnaireService.delete(id);
    }
}
