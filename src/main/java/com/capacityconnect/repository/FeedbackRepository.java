package com.capacityconnect.repository;

import com.capacityconnect.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByTraineeId(Long traineeId);

    List<Feedback> findByCourseId(Long courseId);

    boolean existsByTraineeIdAndCourseId(Long traineeId, Long courseId);
}
