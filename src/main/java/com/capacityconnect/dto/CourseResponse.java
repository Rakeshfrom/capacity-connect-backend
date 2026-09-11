package com.capacityconnect.dto;

import com.capacityconnect.entity.Course;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private Integer durationHours;
    private String department;
    private Course.Level level;
    private Course.Status status;
    private Long trainerId;
    private Long departmentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
