package com.capacityconnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(length = 1000)
    private String url;

    @Column(name = "stored_file_name", length = 255)
    private String storedFileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Type {
        FILE,
        LINK
    }
}
