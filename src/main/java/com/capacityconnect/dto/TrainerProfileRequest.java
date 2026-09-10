package com.capacityconnect.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainerProfileRequest {

    @NotNull
    private Long trainerId;

    private String designation;
    private String department;
    private String specialization;
    private String expertise;
    private Integer experienceYears;
    private String qualifications;
    private String bio;
}
