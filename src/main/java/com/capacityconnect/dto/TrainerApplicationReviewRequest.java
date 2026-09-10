package com.capacityconnect.dto;

import com.capacityconnect.entity.TrainerApplication;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainerApplicationReviewRequest {

    @NotNull
    private TrainerApplication.Status status;

    private String adminComment;
}
