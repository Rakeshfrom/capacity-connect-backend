package com.capacityconnect.repository;

import com.capacityconnect.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    List<AuditLog> findByModuleOrderByCreatedAtDesc(String module);
}
