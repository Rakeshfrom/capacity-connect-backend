package com.capacityconnect.service;

import com.capacityconnect.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageProperties properties;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "ppt", "pptx", "doc", "docx", "txt", "mp4"
    );

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp"
    );

    public String store(MultipartFile file, Long trainerId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (file.getSize() > properties.getMaxSize()) {
            throw new IllegalArgumentException("File size exceeds 10 MB limit");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
        );

        if (originalName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = getExtension(originalName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed");
        }

        try {
            Path trainerDirectory = Paths.get(properties.getLocation())
                    .toAbsolutePath()
                    .normalize()
                    .resolve(String.valueOf(trainerId))
                    .normalize();

            Files.createDirectories(trainerDirectory);

            String storedName = UUID.randomUUID() + "." + extension;
            Path target = trainerDirectory.resolve(storedName).normalize();

            if (!target.startsWith(trainerDirectory)) {
                throw new IllegalArgumentException("Invalid storage path");
            }

            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return trainerId + "/" + storedName;

        } catch (IOException e) {
            throw new IllegalStateException("Could not store file", e);
        }
    }

    public String storeProfilePhoto(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile photo is required");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Profile photo size exceeds 5 MB limit");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename()
        );

        if (originalName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = getExtension(originalName);

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only JPG, JPEG, PNG and WEBP images are allowed");
        }

        try {
            Path profileDirectory = Paths.get(properties.getLocation())
                    .toAbsolutePath()
                    .normalize()
                    .resolve("profiles")
                    .resolve(String.valueOf(userId))
                    .normalize();

            Files.createDirectories(profileDirectory);

            String storedName = UUID.randomUUID() + "." + extension;
            Path target = profileDirectory.resolve(storedName).normalize();

            if (!target.startsWith(profileDirectory)) {
                throw new IllegalArgumentException("Invalid storage path");
            }

            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "profiles/" + userId + "/" + storedName;

        } catch (IOException e) {
            throw new IllegalStateException("Could not store profile photo", e);
        }
    }

    public Path load(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        Path base = Paths.get(properties.getLocation())
                .toAbsolutePath()
                .normalize();

        Path target = base.resolve(storageKey).normalize();

        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("Invalid storage path");
        }

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new IllegalArgumentException("File not found");
        }

        return target;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');

        if (dot < 0 || dot == fileName.length() - 1) {
            throw new IllegalArgumentException("File extension is required");
        }

        return fileName.substring(dot + 1).toLowerCase();
    }
}
