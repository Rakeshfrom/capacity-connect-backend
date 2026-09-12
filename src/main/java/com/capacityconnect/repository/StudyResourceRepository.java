package com.capacityconnect.repository;

import com.capacityconnect.entity.StudyResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyResourceRepository extends JpaRepository<StudyResource, Long> {

    List<StudyResource> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<StudyResource> findByIdAndOwnerId(Long id, Long ownerId);
}
