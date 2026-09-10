package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CompetencyMappingResponse {

    private String competency;
    private List<TrainerMatch> trainers;

    @Getter
    @Builder
    public static class TrainerMatch {
        private Long trainerId;
        private String name;
        private String department;
        private String expertise;
        private Integer experienceYears;
        private String workload;
        private Integer score;
    }
}
