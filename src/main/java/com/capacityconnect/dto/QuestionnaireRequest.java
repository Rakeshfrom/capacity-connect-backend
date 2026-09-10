package com.capacityconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class QuestionnaireRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long courseId;

    @NotNull
    private Long trainerId;

    private LocalDateTime deadline;

    private String status;
}
