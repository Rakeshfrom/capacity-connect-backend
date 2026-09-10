package com.capacityconnect.dto;

import com.capacityconnect.entity.Certificate;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CertificateVerificationResponse {
    private Long id;
    private boolean valid;
    private String certificateNumber;
    private String traineeName;
    private String courseTitle;
    private Certificate.Status status;
    private LocalDateTime issuedAt;
}
