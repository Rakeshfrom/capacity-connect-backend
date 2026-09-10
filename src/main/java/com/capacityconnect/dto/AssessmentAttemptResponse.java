package com.capacityconnect.dto;

import com.capacityconnect.entity.AssessmentAttempt;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AssessmentAttemptResponse {

    private Long id;
    private Long assessmentId;
    private Long traineeId;
    private Integer score;
    private Integer totalMarks;
    private Integer percentage;
    private AssessmentAttempt.Result result;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
}
