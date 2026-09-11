package com.capacityconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseModuleRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    private Integer orderIndex;
}
