package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Boolean active;
}
