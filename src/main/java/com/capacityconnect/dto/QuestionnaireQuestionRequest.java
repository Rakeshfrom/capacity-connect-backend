package com.capacityconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionnaireQuestionRequest {

    @NotNull
    private Long questionnaireId;

    @NotBlank
    private String questionText;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private String questionType;
}
