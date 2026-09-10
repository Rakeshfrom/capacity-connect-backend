package com.capacityconnect.controller;

import com.capacityconnect.dto.TrainerTraineeDetailResponse;
import com.capacityconnect.service.TrainerTraineeDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trainer-trainees")
@RequiredArgsConstructor
public class TrainerTraineeDetailController {

    private final TrainerTraineeDetailService trainerTraineeDetailService;

    @GetMapping("/trainer/{trainerId}/trainee/{traineeId}")
    public TrainerTraineeDetailResponse getTraineeDetail(
            @PathVariable Long trainerId,
            @PathVariable Long traineeId
    ) {
        return trainerTraineeDetailService.getTraineeDetail(
                trainerId,
                traineeId
        );
    }
}
