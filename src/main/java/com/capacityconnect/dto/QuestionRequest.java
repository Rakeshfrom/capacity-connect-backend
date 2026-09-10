package com.capacityconnect.dto;

import com.capacityconnect.entity.Question;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionRequest {

    @NotNull
    @Min(1)
    private Long assessmentId;

    @NotBlank
    private String questionText;

    @NotBlank
    private String optionA;

    @NotBlank
    private String optionB;

    @NotBlank
    private String optionC;

    @NotBlank
    private String optionD;

    @NotNull
    private Question.CorrectOption correctOption;

    @NotNull
    @Min(1)
    private Integer marks;
}
