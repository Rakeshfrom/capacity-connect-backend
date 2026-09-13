package com.capacityconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trainer_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false, unique=true)
    private Long userId;

    @Column(columnDefinition="TEXT")
    private String reason;

    @Column(name="supporting_document_url", length=500)
    private String supportingDocumentUrl;

    @Column(name="supporting_document_key", length=500)
    private String supportingDocumentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(columnDefinition="TEXT")
    private String adminComment;

    @Column(nullable=false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @Column(name="assessment_json", columnDefinition="TEXT")
    private String assessmentJson;

    @Column(name="assessment_score")
    private Integer assessmentScore;

    @Column(name="assessment_passed", nullable=false)
    @Builder.Default
    private boolean assessmentPassed = false;

    private LocalDateTime assessmentAssignedAt;
    private LocalDateTime assessmentCompletedAt;

    public enum Status {
        PENDING, ASSESSMENT_REQUIRED, ASSESSMENT_SUBMITTED, APPROVED, REJECTED
    }

    @PrePersist protected void onCreate() { submittedAt = LocalDateTime.now(); }
}
