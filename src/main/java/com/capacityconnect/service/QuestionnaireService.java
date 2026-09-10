package com.capacityconnect.service;

import com.capacityconnect.dto.QuestionnaireRequest;
import com.capacityconnect.dto.QuestionnaireResponse;
import com.capacityconnect.entity.Questionnaire;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.QuestionnaireRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionnaireService {

    private final QuestionnaireRepository questionnaireRepository;

    public QuestionnaireService(QuestionnaireRepository questionnaireRepository) {
        this.questionnaireRepository = questionnaireRepository;
    }

    public List<QuestionnaireResponse> getAll() {
        return questionnaireRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public QuestionnaireResponse getById(Long id) {
        return toResponse(findQuestionnaire(id));
    }

    public List<QuestionnaireResponse> getByTrainer(Long trainerId) {
        return questionnaireRepository.findByTrainerId(trainerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QuestionnaireResponse> getByCourse(Long courseId) {
        return questionnaireRepository.findByCourseId(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public QuestionnaireResponse create(QuestionnaireRequest request) {
        Questionnaire questionnaire = Questionnaire.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .courseId(request.getCourseId())
                .trainerId(request.getTrainerId())
                .deadline(request.getDeadline())
                .status(parseStatus(request.getStatus()))
                .build();

        return toResponse(questionnaireRepository.save(questionnaire));
    }

    public QuestionnaireResponse update(
            Long id,
            QuestionnaireRequest request) {

        Questionnaire questionnaire = findQuestionnaire(id);

        questionnaire.setTitle(request.getTitle());
        questionnaire.setDescription(request.getDescription());
        questionnaire.setCourseId(request.getCourseId());
        questionnaire.setTrainerId(request.getTrainerId());
        questionnaire.setDeadline(request.getDeadline());

        if (request.getStatus() != null) {
            questionnaire.setStatus(parseStatus(request.getStatus()));
        }

        return toResponse(questionnaireRepository.save(questionnaire));
    }

    public void delete(Long id) {
        questionnaireRepository.delete(findQuestionnaire(id));
    }

    private Questionnaire findQuestionnaire(Long id) {
        return questionnaireRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Questionnaire not found with id: " + id));
    }

    private Questionnaire.Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Questionnaire.Status.DRAFT;
        }

        try {
            return Questionnaire.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid questionnaire status: " + status);
        }
    }

    private QuestionnaireResponse toResponse(Questionnaire questionnaire) {
        return QuestionnaireResponse.builder()
                .id(questionnaire.getId())
                .title(questionnaire.getTitle())
                .description(questionnaire.getDescription())
                .courseId(questionnaire.getCourseId())
                .trainerId(questionnaire.getTrainerId())
                .deadline(questionnaire.getDeadline())
                .status(questionnaire.getStatus().name())
                .build();
    }
}
