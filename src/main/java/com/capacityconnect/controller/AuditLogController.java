package com.capacityconnect.controller;

import com.capacityconnect.dto.AuditLogResponse;
import com.capacityconnect.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAll() {
        return ResponseEntity.ok(auditLogService.getAll());
    }

    @GetMapping("/actor/{actorId}")
    public ResponseEntity<List<AuditLogResponse>> getByActor(
            @PathVariable Long actorId) {
        return ResponseEntity.ok(auditLogService.getByActor(actorId));
    }

    @GetMapping("/module/{module}")
    public ResponseEntity<List<AuditLogResponse>> getByModule(
            @PathVariable String module) {
        return ResponseEntity.ok(auditLogService.getByModule(module));
    }
}
