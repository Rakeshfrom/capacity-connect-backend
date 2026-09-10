package com.capacityconnect.repository;

import com.capacityconnect.entity.TrainerApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerApplicationRepository
        extends JpaRepository<TrainerApplication, Long> {

    Optional<TrainerApplication> findByUserId(Long userId);

    List<TrainerApplication> findByStatus(TrainerApplication.Status status);

    boolean existsByUserId(Long userId);
}
