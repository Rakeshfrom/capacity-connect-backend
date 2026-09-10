package com.capacityconnect.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class AssessmentSubmissionRequest {

    @NotEmpty
    private Map<Long, String> answers;
}
