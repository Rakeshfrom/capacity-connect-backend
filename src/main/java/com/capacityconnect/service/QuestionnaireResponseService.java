package com.capacityconnect.service;

import com.capacityconnect.dto.QuestionnaireResponseDto;
import com.capacityconnect.dto.QuestionnaireResponseRequest;
import com.capacityconnect.entity.QuestionnaireResponse;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.QuestionnaireResponseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionnaireResponseService {

    private final QuestionnaireResponseRepository repository;

    public QuestionnaireResponseService(
            QuestionnaireResponseRepository repository) {
        this.repository = repository;
    }

    public List<QuestionnaireResponseDto> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public QuestionnaireResponseDto getById(Long id) {
        return toResponse(findResponse(id));
    }

    public List<QuestionnaireResponseDto> getByQuestionnaire(
            Long questionnaireId) {
        return repository.findByQuestionnaireId(questionnaireId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QuestionnaireResponseDto> getByTrainee(Long traineeId) {
        return repository.findByTraineeId(traineeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public QuestionnaireResponseDto create(
            QuestionnaireResponseRequest request,
            Long traineeId) {

        QuestionnaireResponse response = QuestionnaireResponse.builder()
                .questionnaireId(request.getQuestionnaireId())
                .questionId(request.getQuestionId())
                .traineeId(traineeId)
                .answer(request.getAnswer())
                .build();

        return toResponse(repository.save(response));
    }

    public void delete(Long id) {
        repository.delete(findResponse(id));
    }

    private QuestionnaireResponse findResponse(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questionnaire response not found with id: " + id));
    }

    private QuestionnaireResponseDto toResponse(
            QuestionnaireResponse response) {

        return QuestionnaireResponseDto.builder()
                .id(response.getId())
                .questionnaireId(response.getQuestionnaireId())
                .questionId(response.getQuestionId())
                .traineeId(response.getTraineeId())
                .answer(response.getAnswer())
                .submittedAt(response.getSubmittedAt())
                .build();
    }
}
