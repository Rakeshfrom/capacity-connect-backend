package com.capacityconnect.dto;

import com.capacityconnect.entity.Enrollment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EnrollmentResponse {

    private Long id;
    private Long traineeId;
    private Long courseId;
    private Enrollment.Status status;
    private Integer progress;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
