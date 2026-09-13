package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter @Builder
public class TrainerAssessmentResponse {
    private String status;
    private Integer score;
    private boolean passed;
    private List<Map<String, Object>> questions;
}
