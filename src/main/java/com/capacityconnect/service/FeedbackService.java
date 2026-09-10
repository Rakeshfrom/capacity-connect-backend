package com.capacityconnect.service;

import com.capacityconnect.dto.FeedbackRequest;
import com.capacityconnect.dto.FeedbackResponse;
import com.capacityconnect.entity.Feedback;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.EnrollmentRepository;
import com.capacityconnect.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final EnrollmentRepository enrollmentRepository;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            EnrollmentRepository enrollmentRepository) {
        this.feedbackRepository = feedbackRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<FeedbackResponse> getAllFeedback() {
        return feedbackRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FeedbackResponse getFeedbackById(Long id) {
        return toResponse(findFeedback(id));
    }

    public List<FeedbackResponse> getByTrainee(Long traineeId) {
        return feedbackRepository.findByTraineeId(traineeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FeedbackResponse> getByCourse(Long courseId) {
        return feedbackRepository.findByCourseId(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FeedbackResponse createFeedback(FeedbackRequest request, Long traineeId) {

        if (!enrollmentRepository.existsByTraineeIdAndCourseId(
                traineeId,
                request.getCourseId())) {
            throw new IllegalArgumentException(
                    "Trainee is not enrolled in this course");
        }

        if (feedbackRepository.existsByTraineeIdAndCourseId(
                traineeId,
                request.getCourseId())) {
            throw new IllegalArgumentException(
                    "Feedback already submitted for this course");
        }

        Feedback feedback = Feedback.builder()
                .traineeId(traineeId)
                .courseId(request.getCourseId())
                .rating(request.getRating())
                .comments(request.getComments())
                .build();

        return toResponse(feedbackRepository.save(feedback));
    }

    public void deleteFeedback(Long id) {
        feedbackRepository.delete(findFeedback(id));
    }

    private Feedback findFeedback(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Feedback not found with id: " + id));
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .traineeId(feedback.getTraineeId())
                .courseId(feedback.getCourseId())
                .rating(feedback.getRating())
                .comments(feedback.getComments())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
