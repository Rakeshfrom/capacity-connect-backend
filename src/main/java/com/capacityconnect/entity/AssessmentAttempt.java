package com.capacityconnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "trainee_id", nullable = false)
    private Long traineeId;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks = 0;

    @Column(nullable = false)
    private Integer percentage = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Result result = Result.PENDING;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }

    public enum Result {
        PENDING,
        PASSED,
        FAILED,
        TERMINATED
    }
}
