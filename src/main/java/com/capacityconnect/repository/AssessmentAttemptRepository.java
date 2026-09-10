package com.capacityconnect.repository;

import com.capacityconnect.entity.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssessmentAttemptRepository
        extends JpaRepository<AssessmentAttempt, Long> {

    List<AssessmentAttempt> findByAssessmentId(Long assessmentId);

    List<AssessmentAttempt> findByTraineeId(Long traineeId);

    List<AssessmentAttempt> findByAssessmentIdAndTraineeId(
            Long assessmentId,
            Long traineeId
    );

    long countByAssessmentId(Long assessmentId);

    long countByTraineeId(Long traineeId);

    @Query("""
        SELECT AVG(a.percentage)
        FROM AssessmentAttempt a
        WHERE a.result = com.capacityconnect.entity.AssessmentAttempt$Result.PASSED
           OR a.result = com.capacityconnect.entity.AssessmentAttempt$Result.FAILED
    """)
    Double findAveragePercentage();

    @Query("""
        SELECT COUNT(a)
        FROM AssessmentAttempt a
        WHERE a.result = com.capacityconnect.entity.AssessmentAttempt$Result.PASSED
           OR a.result = com.capacityconnect.entity.AssessmentAttempt$Result.FAILED
    """)
    Long countSubmittedAttempts();
}
