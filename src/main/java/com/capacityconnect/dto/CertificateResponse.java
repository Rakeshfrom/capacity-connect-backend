package com.capacityconnect.dto;

import com.capacityconnect.entity.Certificate;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CertificateResponse {

    private Long id;
    private Long traineeId;
    private Long courseId;
    private String certificateNumber;
    private Certificate.Status status;
    private LocalDateTime issuedAt;
}
