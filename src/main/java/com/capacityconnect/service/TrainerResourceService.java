package com.capacityconnect.service;

import com.capacityconnect.dto.TrainerResourceRequest;
import com.capacityconnect.dto.TrainerResourceResponse;
import com.capacityconnect.entity.TrainerResource;
import com.capacityconnect.repository.TrainerResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainerResourceService {

    private final TrainerResourceRepository repository;

    public List<TrainerResourceResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public TrainerResourceResponse getById(Long id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found")));
    }

    public List<TrainerResourceResponse> getByTrainer(Long trainerId) {
        return repository.findByTrainerIdAndActiveTrue(trainerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TrainerResourceResponse> getByCourse(Long courseId) {
        return repository.findByCourseId(courseId)
                .stream()
                .filter(TrainerResource::getActive)
                .map(this::toResponse)
                .toList();
    }

    public TrainerResourceResponse create(TrainerResourceRequest request) {
        TrainerResource resource = TrainerResource.builder()
                .trainerId(request.getTrainerId())
                .courseId(request.getCourseId())
                .moduleId(request.getModuleId())
                .title(request.getTitle())
                .description(request.getDescription())
                .resourceType(parseType(request.getResourceType()))
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .fileSize(request.getFileSize())
                .storageKey(request.getStorageKey())
                .active(true)
                .build();

        return toResponse(repository.save(resource));
    }

    public TrainerResourceResponse update(Long id, TrainerResourceRequest request) {
        TrainerResource resource = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

        resource.setCourseId(request.getCourseId());
        resource.setModuleId(request.getModuleId());
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setResourceType(parseType(request.getResourceType()));
        resource.setFileName(request.getFileName());
        resource.setFileType(request.getFileType());
        resource.setFileSize(request.getFileSize());
        resource.setStorageKey(request.getStorageKey());

        return toResponse(repository.save(resource));
    }

    public void delete(Long id) {
        TrainerResource resource = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

        resource.setActive(false);
        repository.save(resource);
    }

    private TrainerResource.ResourceType parseType(String type) {
        try {
            return TrainerResource.ResourceType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid resource type");
        }
    }

    private TrainerResourceResponse toResponse(TrainerResource resource) {
        return TrainerResourceResponse.builder()
                .id(resource.getId())
                .trainerId(resource.getTrainerId())
                .courseId(resource.getCourseId())
                .moduleId(resource.getModuleId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .resourceType(resource.getResourceType().name())
                .fileName(resource.getFileName())
                .fileType(resource.getFileType())
                .fileSize(resource.getFileSize())
                .storageKey(resource.getStorageKey())
                .active(resource.getActive())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
