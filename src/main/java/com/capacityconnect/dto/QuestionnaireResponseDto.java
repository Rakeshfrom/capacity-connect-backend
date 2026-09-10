package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class QuestionnaireResponseDto {

    private Long id;
    private Long questionnaireId;
    private Long questionId;
    private Long traineeId;
    private String answer;
    private LocalDateTime submittedAt;
}
