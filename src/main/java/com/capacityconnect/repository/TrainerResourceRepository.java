package com.capacityconnect.repository;

import com.capacityconnect.entity.TrainerResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainerResourceRepository extends JpaRepository<TrainerResource, Long> {

    List<TrainerResource> findByTrainerId(Long trainerId);

    List<TrainerResource> findByCourseId(Long courseId);

    List<TrainerResource> findByTrainerIdAndActiveTrue(Long trainerId);
}
