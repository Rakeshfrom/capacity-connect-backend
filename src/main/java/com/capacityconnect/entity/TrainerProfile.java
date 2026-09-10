package com.capacityconnect.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trainer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trainer_id", nullable = false, unique = true)
    private Long trainerId;

    private String designation;

    private String department;

    @Column(columnDefinition = "TEXT")
    private String specialization;

    @Column(columnDefinition = "TEXT")
    private String expertise;

    private Integer experienceYears;

    @Column(columnDefinition = "TEXT")
    private String qualifications;

    @Column(columnDefinition = "TEXT")
    private String bio;
}
