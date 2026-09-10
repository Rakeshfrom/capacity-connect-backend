package com.capacityconnect.repository;

import com.capacityconnect.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    List<Certificate> findByTraineeId(Long traineeId);

    List<Certificate> findByCourseId(Long courseId);

    Optional<Certificate> findByTraineeIdAndCourseId(
            Long traineeId,
            Long courseId
    );

    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    long countByStatus(Certificate.Status status);
}
