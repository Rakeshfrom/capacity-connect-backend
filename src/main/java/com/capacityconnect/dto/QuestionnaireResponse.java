package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class QuestionnaireResponse {

    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private Long trainerId;
    private LocalDateTime deadline;
    private String status;
}
