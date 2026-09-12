package com.capacityconnect.controller;

import com.capacityconnect.dto.ProfileRequest;
import com.capacityconnect.dto.LoginRequest;
import com.capacityconnect.dto.LoginResponse;
import com.capacityconnect.service.KeycloakAuthService;
import com.capacityconnect.dto.UserResponse;
import com.capacityconnect.entity.User;
import com.capacityconnect.service.CurrentUserService;
import com.capacityconnect.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaTypeFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final KeycloakAuthService keycloakAuthService;

    public AuthController(
            CurrentUserService currentUserService,
            FileStorageService fileStorageService,
            KeycloakAuthService keycloakAuthService) {
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.keycloakAuthService = keycloakAuthService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return keycloakAuthService.login(
                request.username(),
                request.password()
        );
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        return toResponse(currentUserService.getCurrentUser(authentication));
    }

    @GetMapping("/profile-photo")
    public ResponseEntity<Resource> getProfilePhoto(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);

        if (user.getProfilePicUrl() == null || user.getProfilePicUrl().isBlank()) {
            throw new IllegalArgumentException("Profile photo not found");
        }

        var path = fileStorageService.load(user.getProfilePicUrl());
        Resource file = new FileSystemResource(path);

        MediaType mediaType = MediaTypeFactory
                .getMediaType(path.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(file);
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateProfile(
            Authentication authentication,
            @RequestPart("data") @Valid ProfileRequest request,
            @RequestPart(value = "profilePhoto", required = false)
            MultipartFile profilePhoto) {

        User user = currentUserService.getCurrentUser(authentication);

        user.setPhoneNumber(request.getPhoneNumber());
        user.setDepartment(request.getDepartment());
        user.setQualifications(request.getQualifications());
        user.setSkills(request.getSkills());
        user.setExperienceYears(request.getExperienceYears());
        user.setInterests(request.getInterests());

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String photoKey = fileStorageService.storeProfilePhoto(
                    profilePhoto,
                    user.getId()
            );
            user.setProfilePicUrl(photoKey);
        }

        user.setProfileCompleted(true);

        return toResponse(currentUserService.save(user));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .roles(user.getRoles())
                .department(user.getDepartment())
                .phoneNumber(user.getPhoneNumber())
                .qualifications(user.getQualifications())
                .skills(user.getSkills())
                .experienceYears(user.getExperienceYears())
                .interests(user.getInterests())
                .profilePicUrl(user.getProfilePicUrl())
                .profileCompleted(user.isProfileCompleted())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
