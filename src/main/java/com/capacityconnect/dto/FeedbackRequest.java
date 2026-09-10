package com.capacityconnect.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackRequest {

    @NotNull
    private Long courseId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String comments;
}
