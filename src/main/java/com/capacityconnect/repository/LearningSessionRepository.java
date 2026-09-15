package com.capacityconnect.repository;

import com.capacityconnect.entity.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    List<LearningSession> findByTraineeIdOrderByStartedAtDesc(Long traineeId);

    List<LearningSession> findByTraineeIdAndActiveTrue(Long traineeId);

    List<LearningSession> findByTraineeIdAndStartedAtBetweenOrderByStartedAtAsc(
            Long traineeId,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to
    );
}
