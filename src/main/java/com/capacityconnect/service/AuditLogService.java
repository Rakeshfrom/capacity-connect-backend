package com.capacityconnect.service;

import com.capacityconnect.dto.AuditLogResponse;
import com.capacityconnect.entity.AuditLog;
import com.capacityconnect.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public List<AuditLogResponse> getAll() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AuditLogResponse> getByActor(Long actorId) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AuditLogResponse> getByModule(String module) {
        return auditLogRepository.findByModuleOrderByCreatedAtDesc(module)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AuditLogResponse create(
            Long actorId,
            String actorName,
            String action,
            String module,
            String target,
            AuditLog.Status status) {

        AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .actorName(actorName)
                .action(action)
                .module(module)
                .target(target)
                .status(status)
                .build();

        return toResponse(auditLogRepository.save(log));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .action(log.getAction())
                .module(log.getModule())
                .target(log.getTarget())
                .status(log.getStatus())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
