package com.capacityconnect.controller;

import com.capacityconnect.dto.TrainerApplicationResponse;
import com.capacityconnect.service.TrainerApplicationAdminService;
import org.springframework.web.bind.annotation.*;

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
    public List<TrainerApplicationResponse> getPendingApplications() {
        return service.getPendingApplications();
    }

    @PutMapping("/{id}/approve")
    public TrainerApplicationResponse approve(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {

        return service.approve(id, comment);
    }

    @PutMapping("/{id}/reject")
    public TrainerApplicationResponse reject(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {

        return service.reject(id, comment);
    }
}
