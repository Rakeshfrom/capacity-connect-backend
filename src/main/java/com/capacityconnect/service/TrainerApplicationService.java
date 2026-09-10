package com.capacityconnect.service;

import com.capacityconnect.dto.TrainerApplicationRequest;
import com.capacityconnect.dto.TrainerApplicationResponse;
import com.capacityconnect.dto.TrainerApplicationReviewRequest;
import com.capacityconnect.entity.TrainerApplication;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.TrainerApplicationRepository;
import com.capacityconnect.repository.TrainerProfileRepository;
import com.capacityconnect.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
public class TrainerApplicationService {

    private final TrainerApplicationRepository repository;
    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    public TrainerApplicationService(
            TrainerApplicationRepository repository,
            UserRepository userRepository,
            TrainerProfileRepository trainerProfileRepository,
            CurrentUserService currentUserService,
            FileStorageService fileStorageService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
    }

    public TrainerApplicationResponse submit(
            Authentication authentication,
            TrainerApplicationRequest request,
            MultipartFile supportingDocument) {

        User user = currentUserService.getCurrentUser(authentication);

        String supportingDocumentKey = null;
        if (supportingDocument != null && !supportingDocument.isEmpty()) {
            supportingDocumentKey = fileStorageService.store(
                    supportingDocument,
                    user.getId()
            );
        }

        if (user.getRoles().contains(User.Role.TRAINER)) {
            throw new IllegalStateException("User is already a trainer");
        }

        TrainerApplication existing = repository.findByUserId(user.getId())
                .orElse(null);

        if (existing != null) {
            if (existing.getStatus() == TrainerApplication.Status.PENDING) {
                throw new IllegalStateException(
                        "Trainer application is already pending");
            }

            if (existing.getStatus() == TrainerApplication.Status.APPROVED) {
                throw new IllegalStateException(
                        "Trainer application is already approved");
            }

            existing.setReason(request.getReason());
            existing.setSupportingDocumentUrl(
                    request.getSupportingDocumentUrl());
            if (supportingDocumentKey != null) {
                existing.setSupportingDocumentKey(supportingDocumentKey);
            }
            existing.setStatus(TrainerApplication.Status.PENDING);
            existing.setAdminComment(null);
            existing.setReviewedAt(null);

            return toResponse(repository.save(existing));
        }

        TrainerApplication application = TrainerApplication.builder()
                .userId(user.getId())
                .reason(request.getReason())
                .supportingDocumentUrl(request.getSupportingDocumentUrl())
                .supportingDocumentKey(supportingDocumentKey)
                .status(TrainerApplication.Status.PENDING)
                .build();

        return toResponse(repository.save(application));
    }

    public TrainerApplicationResponse getMyApplication(
            Authentication authentication) {

        User user = currentUserService.getCurrentUser(authentication);

        return repository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElse(null);
    }

    public List<TrainerApplicationResponse> getAllApplications() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TrainerApplicationResponse> getPendingApplications() {
        return repository.findByStatus(TrainerApplication.Status.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TrainerApplicationResponse getApplication(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Trainer application not found: " + id));
    }

    public TrainerApplicationResponse reviewApplication(
            Long id,
            TrainerApplicationReviewRequest request) {

        TrainerApplication application = repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Trainer application not found: " + id));

        if (application.getStatus() != TrainerApplication.Status.PENDING) {
            throw new IllegalStateException(
                    "Only pending applications can be reviewed");
        }

        if (request.getStatus() == TrainerApplication.Status.PENDING) {
            throw new IllegalArgumentException(
                    "Review status must be APPROVED or REJECTED");
        }

        User user = userRepository.findById(application.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found: " + application.getUserId()));

        if (request.getStatus() == TrainerApplication.Status.APPROVED) {
            if (user.getRoles() == null) {
                user.setRoles(new HashSet<>());
            }

            user.getRoles().add(User.Role.TRAINER);
            userRepository.save(user);

            if (!trainerProfileRepository.existsByTrainerId(user.getId())) {
                trainerProfileRepository.save(
                        com.capacityconnect.entity.TrainerProfile.builder()
                                .trainerId(user.getId())
                                .department(user.getDepartment())
                                .experienceYears(user.getExperienceYears())
                                .qualifications(user.getQualifications())
                                .build()
                );
            }
        }

        application.setStatus(request.getStatus());
        application.setAdminComment(request.getAdminComment());
        application.setReviewedAt(LocalDateTime.now());

        return toResponse(repository.save(application));
    }

    private TrainerApplicationResponse toResponse(
            TrainerApplication application) {

        return TrainerApplicationResponse.builder()
                .id(application.getId())
                .userId(application.getUserId())
                .reason(application.getReason())
                .supportingDocumentUrl(
                        application.getSupportingDocumentUrl())
                .supportingDocumentKey(
                        application.getSupportingDocumentKey())
                .status(application.getStatus())
                .adminComment(application.getAdminComment())
                .submittedAt(application.getSubmittedAt())
                .reviewedAt(application.getReviewedAt())
                .build();
    }
}
