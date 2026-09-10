package com.capacityconnect.service;

import com.capacityconnect.dto.CertificateResponse;
import com.capacityconnect.entity.Certificate;
import com.capacityconnect.repository.AssessmentAttemptRepository;
import com.capacityconnect.repository.AssessmentRepository;
import com.capacityconnect.repository.CertificateRepository;
import com.capacityconnect.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AuditLogService auditLogService;

    public List<CertificateResponse> getAll() {
        return certificateRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CertificateResponse> getByTrainee(Long traineeId) {
        return certificateRepository.findByTraineeId(traineeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CertificateResponse issue(Long traineeId, Long courseId) {

        var enrollment = enrollmentRepository
                .findByTraineeIdAndCourseId(traineeId, courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Trainee is not enrolled in this course"
                        ));

        int progress = enrollment.getProgress() == null
                ? 0
                : enrollment.getProgress();

        if (progress < 100) {
            throw new IllegalArgumentException(
                    "Course must be completed before certificate issuance"
            );
        }

        var assessments = assessmentRepository.findAll()
                .stream()
                .filter(assessment ->
                        assessment.getCourseId().equals(courseId))
                .toList();

        if (assessments.isEmpty()) {
            throw new IllegalArgumentException(
                    "No assessment is configured for this course"
            );
        }

        boolean allPassed = assessments.stream().allMatch(assessment ->
                assessmentAttemptRepository
                        .findByAssessmentIdAndTraineeId(
                                assessment.getId(),
                                traineeId
                        )
                        .stream()
                        .anyMatch(attempt ->
                                attempt.getResult() ==
                                        com.capacityconnect.entity.AssessmentAttempt.Result.PASSED
                        )
        );

        if (!allPassed) {
            throw new IllegalArgumentException(
                    "Trainee must pass all course assessments"
            );
        }

        var existing = certificateRepository
                .findByTraineeIdAndCourseId(traineeId, courseId);

        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    "Certificate already exists for this trainee and course"
            );
        }

        var certificate = Certificate.builder()
                .traineeId(traineeId)
                .courseId(courseId)
                .certificateNumber(
                        "CC-" + UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase()
                )
                .status(Certificate.Status.ISSUED)
                .build();

        var savedCertificate = certificateRepository.save(certificate);

        auditLogService.create(
                null,
                "System",
                "Certificate issued",
                "Certification",
                savedCertificate.getCertificateNumber(),
                Certificate.Status.ISSUED == savedCertificate.getStatus()
                        ? com.capacityconnect.entity.AuditLog.Status.COMPLETED
                        : com.capacityconnect.entity.AuditLog.Status.FAILED
        );

        return toResponse(savedCertificate);
    }

    public CertificateResponse revoke(Long id) {
        var certificate = certificateRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Certificate not found"
                        ));

        certificate.setStatus(Certificate.Status.REVOKED);

        var revokedCertificate = certificateRepository.save(certificate);

        auditLogService.create(
                null,
                "System",
                "Certificate revoked",
                "Certification",
                revokedCertificate.getCertificateNumber(),
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );

        return toResponse(revokedCertificate);
    }

    private CertificateResponse toResponse(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .traineeId(certificate.getTraineeId())
                .courseId(certificate.getCourseId())
                .certificateNumber(certificate.getCertificateNumber())
                .status(certificate.getStatus())
                .issuedAt(certificate.getIssuedAt())
                .build();
    }
}
