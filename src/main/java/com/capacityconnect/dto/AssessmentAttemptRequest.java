package com.capacityconnect.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssessmentAttemptRequest {

    @NotNull
    @Min(1)
    private Long assessmentId;
}
