package com.capacityconnect.service;

import com.capacityconnect.dto.CertificateVerificationResponse;
import com.capacityconnect.repository.CertificateRepository;
import com.capacityconnect.repository.CourseRepository;
import com.capacityconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CertificateVerificationService {

    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CertificateVerificationResponse verify(String certificateNumber) {
        var certificate = certificateRepository
                .findByCertificateNumber(certificateNumber.trim().toUpperCase())
                .orElseThrow(() ->
                        new IllegalArgumentException("Certificate not found"));

        var trainee = userRepository.findById(certificate.getTraineeId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Trainee not found"));

        var course = courseRepository.findById(certificate.getCourseId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found"));

        return CertificateVerificationResponse.builder()
                .id(certificate.getId())
                .valid(certificate.getStatus() == com.capacityconnect.entity.Certificate.Status.ISSUED)
                .certificateNumber(certificate.getCertificateNumber())
                .traineeName(
                        trainee.getFirstName() + " " + trainee.getLastName()
                )
                .courseTitle(course.getTitle())
                .status(certificate.getStatus())
                .issuedAt(certificate.getIssuedAt())
                .build();
    }
}
