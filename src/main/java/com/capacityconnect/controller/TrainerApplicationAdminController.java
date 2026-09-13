package com.capacityconnect.controller;

import com.capacityconnect.dto.TrainerApplicationResponse;
import com.capacityconnect.service.TrainerApplicationAdminService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/admin/trainer-applications")
public class TrainerApplicationAdminController {

    private final TrainerApplicationAdminService service;

    public TrainerApplicationAdminController(
            TrainerApplicationAdminService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TrainerApplicationResponse> getPendingApplications() {
        return service.getPendingApplications();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public TrainerApplicationResponse approve(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {

        return service.approve(id, comment);
    }

    @PostMapping("/{id}/assessment")
    @PreAuthorize("hasRole('ADMIN')")
    public TrainerApplicationResponse assignAssessment(@PathVariable Long id, @RequestParam(defaultValue = "MEDIUM") String difficulty, @RequestParam(defaultValue = "10") int questionCount) {
        return service.assignAssessment(id, difficulty, questionCount);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public TrainerApplicationResponse reject(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {

        return service.reject(id, comment);
    }
}
