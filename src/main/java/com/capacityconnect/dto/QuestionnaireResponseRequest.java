package com.capacityconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionnaireResponseRequest {

    @NotNull
    private Long questionnaireId;

    @NotNull
    private Long questionId;

    @NotBlank
    private String answer;
}
