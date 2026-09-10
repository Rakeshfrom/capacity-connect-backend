package com.capacityconnect.controller;

import com.capacityconnect.dto.QuestionnaireQuestionRequest;
import com.capacityconnect.dto.QuestionnaireQuestionResponse;
import com.capacityconnect.service.QuestionnaireQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questionnaire-questions")
public class QuestionnaireQuestionController {

    private final QuestionnaireQuestionService service;

    public QuestionnaireQuestionController(
            QuestionnaireQuestionService service) {
        this.service = service;
    }

    @GetMapping("/questionnaire/{questionnaireId}")
    public List<QuestionnaireQuestionResponse> getByQuestionnaire(
            @PathVariable Long questionnaireId) {
        return service.getByQuestionnaire(questionnaireId);
    }

    @GetMapping("/{id}")
    public QuestionnaireQuestionResponse getById(
            @PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionnaireQuestionResponse create(
            @Valid @RequestBody QuestionnaireQuestionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public QuestionnaireQuestionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody QuestionnaireQuestionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
