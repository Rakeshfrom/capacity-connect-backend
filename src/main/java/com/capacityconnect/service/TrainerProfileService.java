package com.capacityconnect.service;

import com.capacityconnect.dto.TrainerProfileRequest;
import com.capacityconnect.dto.TrainerProfileResponse;
import com.capacityconnect.entity.TrainerProfile;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.TrainerProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class TrainerProfileService {

    private final TrainerProfileRepository trainerProfileRepository;

    public TrainerProfileService(TrainerProfileRepository trainerProfileRepository) {
        this.trainerProfileRepository = trainerProfileRepository;
    }

    public TrainerProfileResponse getByTrainerId(Long trainerId) {
        return toResponse(
                trainerProfileRepository.findByTrainerId(trainerId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Trainer profile not found for trainer: " + trainerId))
        );
    }

    public TrainerProfileResponse createProfile(TrainerProfileRequest request) {
        if (trainerProfileRepository.existsByTrainerId(request.getTrainerId())) {
            throw new IllegalArgumentException(
                    "Trainer profile already exists");
        }

        TrainerProfile profile = TrainerProfile.builder()
                .trainerId(request.getTrainerId())
                .designation(request.getDesignation())
                .department(request.getDepartment())
                .specialization(request.getSpecialization())
                .expertise(request.getExpertise())
                .experienceYears(request.getExperienceYears())
                .qualifications(request.getQualifications())
                .bio(request.getBio())
                .build();

        return toResponse(trainerProfileRepository.save(profile));
    }

    public TrainerProfileResponse updateProfile(
            Long trainerId,
            TrainerProfileRequest request) {

        TrainerProfile profile = trainerProfileRepository.findByTrainerId(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trainer profile not found for trainer: " + trainerId));

        profile.setDesignation(request.getDesignation());
        profile.setDepartment(request.getDepartment());
        profile.setSpecialization(request.getSpecialization());
        profile.setExpertise(request.getExpertise());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setQualifications(request.getQualifications());
        profile.setBio(request.getBio());

        return toResponse(trainerProfileRepository.save(profile));
    }

    private TrainerProfileResponse toResponse(TrainerProfile profile) {
        return TrainerProfileResponse.builder()
                .id(profile.getId())
                .trainerId(profile.getTrainerId())
                .designation(profile.getDesignation())
                .department(profile.getDepartment())
                .specialization(profile.getSpecialization())
                .expertise(profile.getExpertise())
                .experienceYears(profile.getExperienceYears())
                .qualifications(profile.getQualifications())
                .bio(profile.getBio())
                .build();
    }
}
