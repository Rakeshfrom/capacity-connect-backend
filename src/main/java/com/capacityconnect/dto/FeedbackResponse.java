package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {

    private Long id;
    private Long traineeId;
    private Long courseId;
    private Integer rating;
    private String comments;
    private LocalDateTime createdAt;
}
