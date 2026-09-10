package com.capacityconnect.service;

import com.capacityconnect.dto.QuestionRequest;
import com.capacityconnect.dto.QuestionResponse;
import com.capacityconnect.dto.TraineeQuestionResponse;
import com.capacityconnect.entity.Question;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<QuestionResponse> getAllQuestions() {
        return questionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public QuestionResponse getQuestionById(Long id) {
        return toResponse(findQuestion(id));
    }

    public List<QuestionResponse> getByAssessment(Long assessmentId) {
        return questionRepository.findByAssessmentId(assessmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TraineeQuestionResponse> getTraineeQuestionsByAssessment(Long assessmentId) {
        return questionRepository.findByAssessmentId(assessmentId)
                .stream()
                .map(this::toTraineeResponse)
                .toList();
    }

    public QuestionResponse createQuestion(QuestionRequest request) {
        Question question = Question.builder()
                .assessmentId(request.getAssessmentId())
                .questionText(request.getQuestionText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctOption(request.getCorrectOption())
                .marks(request.getMarks())
                .build();

        return toResponse(questionRepository.save(question));
    }

    public QuestionResponse updateQuestion(
            Long id,
            QuestionRequest request) {

        Question question = findQuestion(id);

        question.setAssessmentId(request.getAssessmentId());
        question.setQuestionText(request.getQuestionText());
        question.setOptionA(request.getOptionA());
        question.setOptionB(request.getOptionB());
        question.setOptionC(request.getOptionC());
        question.setOptionD(request.getOptionD());
        question.setCorrectOption(request.getCorrectOption());
        question.setMarks(request.getMarks());

        return toResponse(questionRepository.save(question));
    }

    public void deleteQuestion(Long id) {
        Question question = findQuestion(id);
        questionRepository.delete(question);
    }

    private Question findQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Question not found with id: " + id));
    }

    private TraineeQuestionResponse toTraineeResponse(Question question) {
        return TraineeQuestionResponse.builder()
                .id(question.getId())
                .assessmentId(question.getAssessmentId())
                .questionText(question.getQuestionText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .marks(question.getMarks())
                .build();
    }

    private QuestionResponse toResponse(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .assessmentId(question.getAssessmentId())
                .questionText(question.getQuestionText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .correctOption(question.getCorrectOption())
                .marks(question.getMarks())
                .createdAt(question.getCreatedAt())
                .build();
    }
}
