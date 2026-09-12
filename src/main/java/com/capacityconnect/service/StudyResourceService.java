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
    private final AiResourceService aiResourceService;

    @Value("${trainee-storage.location:uploads/trainee-resources}")
    private String storageLocation;

    @Value("${file.storage.max-size:10485760}")
    private long maxSize;

    public StudyResourceService(
            StudyResourceRepository repository,
            CurrentUserService currentUserService,
            AiResourceService aiResourceService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.aiResourceService = aiResourceService;
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

        String contentText = null;
        try {
            contentText = aiResourceService.extractText(file);
        } catch (Exception ignored) {
        }

        StudyResource resource = StudyResource.builder()
                .title(title)
                .description(description)
                .type(StudyResource.Type.FILE)
                .ownerId(user.getId())
                .storedFileName(stored)
                .originalFileName(original)
                .contentType(file.getContentType())
                .contentText(contentText)
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

        String contentText = null;
        try {
            contentText = aiResourceService.extractTextFromUrl(url);
        } catch (Exception ignored) {
        }

        StudyResource resource = StudyResource.builder()
                .title(title)
                .description(description)
                .type(StudyResource.Type.LINK)
                .ownerId(user.getId())
                .url(url)
                .contentText(contentText)
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

    public String getAiContent(
            Long id,
            org.springframework.security.core.Authentication authentication) throws Exception {

        StudyResource resource = getMineById(id, authentication);

        if (resource.getContentText() != null && !resource.getContentText().isBlank()) {
            return resource.getContentText();
        }

        if (resource.getType() == StudyResource.Type.LINK
                && resource.getUrl() != null
                && !resource.getUrl().isBlank()) {
            String text = aiResourceService.extractTextFromUrl(resource.getUrl());
            resource.setContentText(text);
            repository.save(resource);
            return text;
        }

        if (resource.getType() == StudyResource.Type.FILE
                && resource.getStoredFileName() != null) {

            Path current = Paths.get(storageLocation)
                    .toAbsolutePath()
                    .normalize()
                    .resolve(resource.getStoredFileName());

            Path legacy = Paths.get("uploads/trainer-resources")
                    .toAbsolutePath()
                    .normalize()
                    .resolve(resource.getStoredFileName());

            Path filePath = Files.exists(current) ? current : legacy;

            if (Files.exists(filePath)) {
                String text = aiResourceService.extractTextFromBytes(
                        Files.readAllBytes(filePath),
                        resource.getOriginalFileName()
                );
                resource.setContentText(text);
                repository.save(resource);
                return text;
            }
        }

        return resource.getDescription() == null ? "" : resource.getDescription();
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
