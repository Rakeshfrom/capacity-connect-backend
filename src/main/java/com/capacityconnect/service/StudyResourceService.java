package com.capacityconnect.service;

import com.capacityconnect.dto.StudyResourceResponse;
import com.capacityconnect.entity.StudyResource;
import com.capacityconnect.entity.User;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.StudyResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class StudyResourceService {

    private final StudyResourceRepository repository;
    private final CurrentUserService currentUserService;

    @Value("${trainee-storage.location:uploads/trainee-resources}")
    private String storageLocation;

    @Value("${file.storage.max-size:10485760}")
    private long maxSize;

    public StudyResourceService(
            StudyResourceRepository repository,
            CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    public List<StudyResourceResponse> getMine(
            org.springframework.security.core.Authentication authentication) {

        User user = currentUserService.getCurrentUser(authentication);

        return repository.findByOwnerIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public StudyResourceResponse createFile(
            String title,
            String description,
            MultipartFile file,
            org.springframework.security.core.Authentication authentication)
            throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File exceeds the 10 MB limit");
        }

        User user = currentUserService.getCurrentUser(authentication);

        Path directory = Paths.get(storageLocation).toAbsolutePath().normalize();
        Files.createDirectories(directory);

        String original = file.getOriginalFilename() == null
                ? "resource"
                : Paths.get(file.getOriginalFilename()).getFileName().toString();

        String stored = UUID.randomUUID() + "-" + original;

        Files.copy(
                file.getInputStream(),
                directory.resolve(stored),
                StandardCopyOption.REPLACE_EXISTING
        );

        StudyResource resource = StudyResource.builder()
                .title(title)
                .description(description)
                .type(StudyResource.Type.FILE)
                .ownerId(user.getId())
                .storedFileName(stored)
                .originalFileName(original)
                .contentType(file.getContentType())
                .build();

        return toResponse(repository.save(resource));
    }

    public StudyResourceResponse createLink(
            String title,
            String description,
            String url,
            org.springframework.security.core.Authentication authentication) {

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }

        User user = currentUserService.getCurrentUser(authentication);

        StudyResource resource = StudyResource.builder()
                .title(title)
                .description(description)
                .type(StudyResource.Type.LINK)
                .ownerId(user.getId())
                .url(url)
                .build();

        return toResponse(repository.save(resource));
    }

    public StudyResource getMineById(
            Long id,
            org.springframework.security.core.Authentication authentication) {

        User user = currentUserService.getCurrentUser(authentication);

        return repository.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Study resource not found"));
    }

    public Path getFilePath(
            Long id,
            org.springframework.security.core.Authentication authentication) {

        StudyResource resource = getMineById(id, authentication);

        if (resource.getType() != StudyResource.Type.FILE) {
            throw new IllegalArgumentException("Resource is not a file");
        }

        return Paths.get(storageLocation)
                .toAbsolutePath()
                .normalize()
                .resolve(resource.getStoredFileName());
    }

    public void delete(
            Long id,
            org.springframework.security.core.Authentication authentication)
            throws IOException {

        StudyResource resource = getMineById(id, authentication);

        if (resource.getStoredFileName() != null) {
            Files.deleteIfExists(
                    Paths.get(storageLocation)
                            .toAbsolutePath()
                            .normalize()
                            .resolve(resource.getStoredFileName())
            );
        }

        repository.delete(resource);
    }

    private StudyResourceResponse toResponse(StudyResource r) {
        return new StudyResourceResponse(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                r.getDepartment(),
                r.getType(),
                r.getUrl(),
                r.getOriginalFileName(),
                r.getContentType(),
                r.getCreatedAt()
        );
    }
}
