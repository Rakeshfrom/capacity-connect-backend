package com.capacityconnect.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter @Setter
public class TrainerAssessmentSubmissionRequest {
    private Map<String, String> answers;
}
