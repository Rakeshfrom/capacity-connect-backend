package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TraineeQuestionResponse {

    private Long id;
    private Long assessmentId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer marks;
}
