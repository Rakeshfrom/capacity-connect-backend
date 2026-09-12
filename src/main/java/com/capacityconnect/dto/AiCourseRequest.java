package com.capacityconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiCourseRequest {
    @NotBlank
    private String topic;

    private String context;
    private String level = "BEGINNER";
    private Integer moduleCount = 5;
}
