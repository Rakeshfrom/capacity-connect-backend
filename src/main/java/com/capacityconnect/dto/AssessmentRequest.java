package com.capacityconnect.dto;

import com.capacityconnect.entity.Assessment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssessmentRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    @NotNull
    @Min(1)
    private Long courseId;

    @NotNull
    @Min(1)
    private Integer timeLimitMinutes;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer passingPercentage;

    @NotNull
    private Assessment.Status status;
}
