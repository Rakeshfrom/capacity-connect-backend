package com.capacityconnect.controller;

import com.capacityconnect.dto.AdminDashboardResponse;
import com.capacityconnect.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin-dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService service;

    @GetMapping
    public AdminDashboardResponse getDashboard() {
        return service.getDashboard();
    }
}
