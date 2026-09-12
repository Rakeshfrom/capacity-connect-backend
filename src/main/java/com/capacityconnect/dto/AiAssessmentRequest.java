package com.capacityconnect.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiAssessmentRequest {

    @NotBlank
    private String topic;

    private String context;

    @Min(1)
    @Max(30)
    private Integer questionCount = 5;

    private String difficulty = "MEDIUM";
}
