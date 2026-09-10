package com.capacityconnect.dto;

import com.capacityconnect.entity.TrainerApplication;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TrainerApplicationResponse {

    private Long id;
    private Long userId;
    private String reason;
    private String supportingDocumentUrl;
    private String supportingDocumentKey;
    private TrainerApplication.Status status;
    private String adminComment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
