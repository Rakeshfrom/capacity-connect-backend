package com.capacityconnect.service;

import com.capacityconnect.dto.QuestionnaireQuestionRequest;
import com.capacityconnect.dto.QuestionnaireQuestionResponse;
import com.capacityconnect.entity.QuestionnaireQuestion;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.QuestionnaireQuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionnaireQuestionService {

    private final QuestionnaireQuestionRepository repository;

    public QuestionnaireQuestionService(QuestionnaireQuestionRepository repository) {
        this.repository = repository;
    }

    public List<QuestionnaireQuestionResponse> getByQuestionnaire(
            Long questionnaireId) {
        return repository.findByQuestionnaireId(questionnaireId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public QuestionnaireQuestionResponse getById(Long id) {
        return toResponse(findQuestion(id));
    }

    public QuestionnaireQuestionResponse create(
            QuestionnaireQuestionRequest request) {

        QuestionnaireQuestion question = QuestionnaireQuestion.builder()
                .questionnaireId(request.getQuestionnaireId())
                .questionText(request.getQuestionText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .questionType(parseType(request.getQuestionType()))
                .build();

        return toResponse(repository.save(question));
    }

    public QuestionnaireQuestionResponse update(
            Long id,
            QuestionnaireQuestionRequest request) {

        QuestionnaireQuestion question = findQuestion(id);

        question.setQuestionText(request.getQuestionText());
        question.setOptionA(request.getOptionA());
        question.setOptionB(request.getOptionB());
        question.setOptionC(request.getOptionC());
        question.setOptionD(request.getOptionD());

        if (request.getQuestionType() != null) {
            question.setQuestionType(parseType(request.getQuestionType()));
        }

        return toResponse(repository.save(question));
    }

    public void delete(Long id) {
        repository.delete(findQuestion(id));
    }

    private QuestionnaireQuestion findQuestion(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questionnaire question not found with id: " + id));
    }

    private QuestionnaireQuestion.QuestionType parseType(String type) {
        if (type == null || type.isBlank()) {
            return QuestionnaireQuestion.QuestionType.MCQ;
        }

        try {
            return QuestionnaireQuestion.QuestionType.valueOf(
                    type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid question type: " + type);
        }
    }

    private QuestionnaireQuestionResponse toResponse(
            QuestionnaireQuestion question) {

        return QuestionnaireQuestionResponse.builder()
                .id(question.getId())
                .questionnaireId(question.getQuestionnaireId())
                .questionText(question.getQuestionText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .questionType(question.getQuestionType().name())
                .build();
    }
}
