package com.capacityconnect.controller;

import com.capacityconnect.dto.TrainerProfileRequest;
import com.capacityconnect.dto.TrainerProfileResponse;
import com.capacityconnect.service.TrainerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trainer-profiles")
public class TrainerProfileController {

    private final TrainerProfileService trainerProfileService;

    public TrainerProfileController(
            TrainerProfileService trainerProfileService) {
        this.trainerProfileService = trainerProfileService;
    }

    @GetMapping("/{trainerId}")
    public TrainerProfileResponse getProfile(
            @PathVariable Long trainerId) {
        return trainerProfileService.getByTrainerId(trainerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainerProfileResponse createProfile(
            @Valid @RequestBody TrainerProfileRequest request) {
        return trainerProfileService.createProfile(request);
    }

    @PutMapping("/{trainerId}")
    public TrainerProfileResponse updateProfile(
            @PathVariable Long trainerId,
            @Valid @RequestBody TrainerProfileRequest request) {
        return trainerProfileService.updateProfile(trainerId, request);
    }
}
