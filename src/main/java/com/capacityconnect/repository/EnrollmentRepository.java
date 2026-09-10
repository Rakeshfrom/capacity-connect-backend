package com.capacityconnect.repository;

import com.capacityconnect.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByTraineeIdAndCourseId(Long traineeId, Long courseId);

    List<Enrollment> findByTraineeId(Long traineeId);

    List<Enrollment> findByCourseId(Long courseId);

    List<Enrollment> findByStatus(Enrollment.Status status);

    boolean existsByTraineeIdAndCourseId(Long traineeId, Long courseId);

    @Query("SELECT AVG(e.progress) FROM Enrollment e")
    Double findAverageProgress();

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.status = com.capacityconnect.entity.Enrollment$Status.COMPLETED")
    Long countCompleted();
}
