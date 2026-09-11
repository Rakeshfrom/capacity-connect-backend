package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TrainerResourceResponse {

    private Long id;
    private Long trainerId;
    private Long courseId;

    private Long moduleId;
    private String title;
    private String description;
    private String resourceType;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String storageKey;
    private Boolean active;
    private LocalDateTime createdAt;
}
