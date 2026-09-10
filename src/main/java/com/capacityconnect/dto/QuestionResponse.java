package com.capacityconnect.dto;

import com.capacityconnect.entity.Question;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class QuestionResponse {

    private Long id;
    private Long assessmentId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Question.CorrectOption correctOption;
    private Integer marks;
    private LocalDateTime createdAt;
}
