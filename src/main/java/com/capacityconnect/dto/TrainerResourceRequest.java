package com.capacityconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainerResourceRequest {

    @NotNull
    private Long trainerId;

    private Long courseId;

    private Long moduleId;

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String resourceType;

    @NotBlank
    private String fileName;

    private String fileType;

    private Long fileSize;

    private String storageKey;
}
