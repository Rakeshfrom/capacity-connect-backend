package com.capacityconnect.controller;

import com.capacityconnect.dto.CompetencyMappingResponse;
import com.capacityconnect.service.CompetencyMappingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/competency-mapping")
public class CompetencyMappingController {

    private final CompetencyMappingService competencyMappingService;

    public CompetencyMappingController(
            CompetencyMappingService competencyMappingService
    ) {
        this.competencyMappingService = competencyMappingService;
    }

    @GetMapping
    public CompetencyMappingResponse findSuitableTrainers(
            @RequestParam String competency
    ) {
        return competencyMappingService.findSuitableTrainers(competency);
    }
}
