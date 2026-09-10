package com.capacityconnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "certificates",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_certificate_trainee_course",
        columnNames = {"trainee_id", "course_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trainee_id", nullable = false)
    private Long traineeId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "certificate_number", nullable = false, unique = true)
    private String certificateNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ISSUED;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    public enum Status {
        ISSUED,
        REVOKED
    }

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }
}
