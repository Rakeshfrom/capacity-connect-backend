package com.capacityconnect.service;

import com.capacityconnect.dto.TrainerApplicationResponse;
import com.capacityconnect.entity.TrainerApplication;
import com.capacityconnect.entity.TrainerProfile;
import com.capacityconnect.entity.User;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.TrainerApplicationRepository;
import com.capacityconnect.repository.TrainerProfileRepository;
import com.capacityconnect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainerApplicationAdminService {

    private final TrainerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;

    public TrainerApplicationAdminService(
            TrainerApplicationRepository applicationRepository,
            UserRepository userRepository,
            TrainerProfileRepository trainerProfileRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
    }

    public List<TrainerApplicationResponse> getPendingApplications() {
        return applicationRepository
                .findByStatus(TrainerApplication.Status.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TrainerApplicationResponse approve(
            Long applicationId,
            String adminComment) {

        TrainerApplication application = findApplication(applicationId);

        if (application.getStatus() != TrainerApplication.Status.PENDING) {
            throw new IllegalStateException(
                    "Only pending applications can be approved");
        }

        User user = userRepository.findById(application.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + application.getUserId()));

        user.getRoles().add(User.Role.TRAINER);
        user.setRole(User.Role.TRAINER);
        userRepository.save(user);

        if (!trainerProfileRepository.existsByTrainerId(user.getId())) {
            TrainerProfile profile = TrainerProfile.builder()
                    .trainerId(user.getId())
                    .department(user.getDepartment())
                    .experienceYears(user.getExperienceYears())
                    .qualifications(user.getQualifications())
                    .build();

            trainerProfileRepository.save(profile);
        }

        application.setStatus(TrainerApplication.Status.APPROVED);
        application.setAdminComment(adminComment);
        application.setReviewedAt(LocalDateTime.now());

        return toResponse(applicationRepository.save(application));
    }

    public TrainerApplicationResponse reject(
            Long applicationId,
            String adminComment) {

        TrainerApplication application = findApplication(applicationId);

        if (application.getStatus() != TrainerApplication.Status.PENDING) {
            throw new IllegalStateException(
                    "Only pending applications can be rejected");
        }

        application.setStatus(TrainerApplication.Status.REJECTED);
        application.setAdminComment(adminComment);
        application.setReviewedAt(LocalDateTime.now());

        return toResponse(applicationRepository.save(application));
    }

    private TrainerApplication findApplication(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trainer application not found: " + id));
    }

    private TrainerApplicationResponse toResponse(
            TrainerApplication application) {

        return TrainerApplicationResponse.builder()
                .id(application.getId())
                .userId(application.getUserId())
                .reason(application.getReason())
                .supportingDocumentUrl(
                        application.getSupportingDocumentUrl())
                .status(application.getStatus())
                .adminComment(application.getAdminComment())
                .submittedAt(application.getSubmittedAt())
                .reviewedAt(application.getReviewedAt())
                .build();
    }
}
