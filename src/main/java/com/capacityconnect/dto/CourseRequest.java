package com.capacityconnect.dto;

import com.capacityconnect.entity.Course;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    @Size(max = 100)
    private String category;

    @Min(1)
    private Integer durationHours;

    @Size(max = 150)
    private String department;

    @NotNull
    private Course.Level level;

    @NotNull
    private Course.Status status;

    private Long trainerId;

    @NotNull
    private Long departmentId;
}
