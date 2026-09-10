package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrainerTraineeResponse {

    private Long traineeId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Long courseId;
    private String courseTitle;
    private String enrollmentStatus;
    private Integer progress;
}
