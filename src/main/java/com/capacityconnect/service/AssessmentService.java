package com.capacityconnect.service;

import com.capacityconnect.dto.AssessmentRequest;
import com.capacityconnect.dto.AssessmentResponse;
import com.capacityconnect.entity.Assessment;
import com.capacityconnect.exception.DuplicateResourceException;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.AssessmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AuditLogService auditLogService;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            AuditLogService auditLogService) {
        this.assessmentRepository = assessmentRepository;
        this.auditLogService = auditLogService;
    }

    public List<AssessmentResponse> getAllAssessments() {
        return assessmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AssessmentResponse getAssessmentById(Long id) {
        return toResponse(findAssessment(id));
    }

    public List<AssessmentResponse> getByCourse(Long courseId) {
        return assessmentRepository.findByCourseId(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AssessmentResponse> getByStatus(Assessment.Status status) {
        return assessmentRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AssessmentResponse createAssessment(AssessmentRequest request) {
        if (assessmentRepository.existsByTitleIgnoreCaseAndCourseId(
                request.getTitle(), request.getCourseId())) {
            throw new DuplicateResourceException(
                    "Assessment title already exists for this course: "
                            + request.getTitle());
        }

        Assessment assessment = Assessment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .courseId(request.getCourseId())
                .moduleId(request.getModuleId())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passingPercentage(request.getPassingPercentage())
                .status(request.getStatus())
                .build();

        Assessment savedAssessment = assessmentRepository.save(assessment);

        auditLogService.create(
                null,
                "System",
                "Assessment created",
                "Assessment Management",
                savedAssessment.getTitle(),
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );

        return toResponse(savedAssessment);
    }

    public AssessmentResponse updateAssessment(
            Long id,
            AssessmentRequest request) {

        Assessment assessment = findAssessment(id);

        assessmentRepository.findByCourseId(request.getCourseId())
                .stream()
                .filter(existing -> !existing.getId().equals(id))
                .filter(existing ->
                        existing.getTitle().equalsIgnoreCase(request.getTitle()))
                .findFirst()
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Assessment title already exists for this course: "
                                    + request.getTitle());
                });

        assessment.setTitle(request.getTitle());
        assessment.setDescription(request.getDescription());
        assessment.setCourseId(request.getCourseId());
        assessment.setModuleId(request.getModuleId());
        assessment.setTimeLimitMinutes(request.getTimeLimitMinutes());
        assessment.setPassingPercentage(request.getPassingPercentage());
        assessment.setStatus(request.getStatus());

        Assessment updatedAssessment = assessmentRepository.save(assessment);

        auditLogService.create(
                null,
                "System",
                "Assessment updated",
                "Assessment Management",
                updatedAssessment.getTitle(),
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );

        return toResponse(updatedAssessment);
    }

    public void deleteAssessment(Long id) {
        Assessment assessment = findAssessment(id);
        String target = assessment.getTitle();

        assessmentRepository.delete(assessment);

        auditLogService.create(
                null,
                "System",
                "Assessment deleted",
                "Assessment Management",
                target,
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );
    }

    private Assessment findAssessment(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assessment not found with id: " + id));
    }

    private AssessmentResponse toResponse(Assessment assessment) {
        return AssessmentResponse.builder()
                .id(assessment.getId())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .courseId(assessment.getCourseId())
                .moduleId(assessment.getModuleId())
                .timeLimitMinutes(assessment.getTimeLimitMinutes())
                .passingPercentage(assessment.getPassingPercentage())
                .status(assessment.getStatus())
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .build();
    }
}
