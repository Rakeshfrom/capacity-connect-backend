package com.capacityconnect.controller;

import com.capacityconnect.dto.TrainerAnalyticsResponse;
import com.capacityconnect.service.TrainerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trainer-analytics")
@RequiredArgsConstructor
public class TrainerAnalyticsController {

    private final TrainerAnalyticsService trainerAnalyticsService;

    @GetMapping("/trainer/{trainerId}")
    public TrainerAnalyticsResponse getAnalytics(
            @PathVariable Long trainerId
    ) {
        return trainerAnalyticsService.getAnalytics(trainerId);
    }
}
