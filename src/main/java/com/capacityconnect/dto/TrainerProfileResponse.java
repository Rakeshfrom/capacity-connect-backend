package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrainerProfileResponse {

    private Long id;
    private Long trainerId;
    private String designation;
    private String department;
    private String specialization;
    private String expertise;
    private Integer experienceYears;
    private String qualifications;
    private String bio;
}
