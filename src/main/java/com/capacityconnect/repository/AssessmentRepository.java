package com.capacityconnect.repository;

import com.capacityconnect.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByCourseId(Long courseId);

    List<Assessment> findByStatus(Assessment.Status status);

    boolean existsByTitleIgnoreCaseAndCourseId(
            String title,
            Long courseId
    );
}
