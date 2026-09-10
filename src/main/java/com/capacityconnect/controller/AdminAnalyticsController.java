package com.capacityconnect.controller;

import com.capacityconnect.dto.AdminAnalyticsResponse;
import com.capacityconnect.service.AdminAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(
            AdminAnalyticsService adminAnalyticsService
    ) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping
    public AdminAnalyticsResponse getAnalytics() {
        return adminAnalyticsService.getAnalytics();
    }
}
