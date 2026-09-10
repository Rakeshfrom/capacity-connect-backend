package com.capacityconnect.dto;

import com.capacityconnect.entity.AuditLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponse {

    private Long id;
    private Long actorId;
    private String actorName;
    private String action;
    private String module;
    private String target;
    private AuditLog.Status status;
    private LocalDateTime createdAt;
}
