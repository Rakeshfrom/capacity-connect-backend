package com.capacityconnect.controller;

import com.capacityconnect.dto.TrainerResourceRequest;
import com.capacityconnect.dto.TrainerResourceResponse;
import com.capacityconnect.service.FileStorageService;
import com.capacityconnect.service.TrainerResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/trainer-resources")
@RequiredArgsConstructor
public class TrainerResourceController {

    private final TrainerResourceService service;
    private final FileStorageService fileStorageService;

    @GetMapping
    public List<TrainerResourceResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public TrainerResourceResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/trainer/{trainerId}")
    public List<TrainerResourceResponse> getByTrainer(
            @PathVariable Long trainerId) {
        return service.getByTrainer(trainerId);
    }

    @GetMapping("/course/{courseId}")
    public List<TrainerResourceResponse> getByCourse(
            @PathVariable Long courseId) {
        return service.getByCourse(courseId);
    }

    @PostMapping
    public TrainerResourceResponse create(
            @Valid @RequestBody TrainerResourceRequest request) {
        return service.create(request);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TrainerResourceResponse upload(
            @RequestParam Long trainerId,
            @RequestParam(required = false) Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String resourceType,
            @RequestParam("file") MultipartFile file) {

        String storageKey = fileStorageService.store(file, trainerId);

        TrainerResourceRequest request = new TrainerResourceRequest();
        request.setTrainerId(trainerId);
        request.setCourseId(courseId);
        request.setTitle(title);
        request.setDescription(description);
        request.setResourceType(resourceType);
        request.setFileName(file.getOriginalFilename());
        request.setFileType(file.getContentType());
        request.setFileSize(file.getSize());
        request.setStorageKey(storageKey);

        return service.create(request);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        TrainerResourceResponse resource = service.getById(id);

        if (!Boolean.TRUE.equals(resource.getActive())) {
            throw new IllegalArgumentException("Resource is inactive");
        }

        var path = fileStorageService.load(resource.getStorageKey());
        Resource file = new FileSystemResource(path);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFileName() + "\""
                )
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        resource.getFileType() == null
                                ? "application/octet-stream"
                                : resource.getFileType()
                )
                .body(file);
    }

    @PutMapping("/{id}")
    public TrainerResourceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TrainerResourceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
