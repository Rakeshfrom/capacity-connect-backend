package com.capacityconnect.controller;

import com.capacityconnect.dto.TrainerTraineeResponse;
import com.capacityconnect.service.TrainerTraineeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer-trainees")
@RequiredArgsConstructor
public class TrainerTraineeController {

    private final TrainerTraineeService trainerTraineeService;

    @GetMapping("/trainer/{trainerId}")
    public List<TrainerTraineeResponse> getTraineesByTrainer(
            @PathVariable Long trainerId
    ) {
        return trainerTraineeService.getTraineesByTrainer(trainerId);
    }
}
