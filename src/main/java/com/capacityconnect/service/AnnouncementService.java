package com.capacityconnect.service;

import com.capacityconnect.dto.AnnouncementRequest;
import com.capacityconnect.dto.AnnouncementResponse;
import com.capacityconnect.entity.Announcement;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AuditLogService auditLogService;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            AuditLogService auditLogService
    ) {
        this.announcementRepository = announcementRepository;
        this.auditLogService = auditLogService;
    }

    public List<AnnouncementResponse> getAll() {
        return announcementRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AnnouncementResponse> getPublished() {
        return announcementRepository
                .findByStatusOrderByCreatedAtDesc(Announcement.Status.PUBLISHED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AnnouncementResponse getById(Long id) {
        return toResponse(findAnnouncement(id));
    }

    public AnnouncementResponse create(AnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .audience(request.getAudience())
                .status(request.getStatus())
                .type(request.getType())
                .build();

        Announcement saved = announcementRepository.save(announcement);

        auditLogService.create(
                null,
                "System",
                "Announcement created",
                "Announcement Management",
                saved.getTitle(),
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );

        return toResponse(saved);
    }

    public AnnouncementResponse update(
            Long id,
            AnnouncementRequest request
    ) {
        Announcement announcement = findAnnouncement(id);

        announcement.setTitle(request.getTitle());
        announcement.setAudience(request.getAudience());
        announcement.setStatus(request.getStatus());
        announcement.setType(request.getType());

        Announcement updated = announcementRepository.save(announcement);

        auditLogService.create(
                null,
                "System",
                "Announcement updated",
                "Announcement Management",
                updated.getTitle(),
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );

        return toResponse(updated);
    }

    public void delete(Long id) {
        Announcement announcement = findAnnouncement(id);
        String target = announcement.getTitle();

        announcementRepository.delete(announcement);

        auditLogService.create(
                null,
                "System",
                "Announcement deleted",
                "Announcement Management",
                target,
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );
    }

    private Announcement findAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Announcement not found with id: " + id
                        ));
    }

    private AnnouncementResponse toResponse(Announcement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .audience(announcement.getAudience())
                .status(announcement.getStatus())
                .type(announcement.getType())
                .publishedAt(announcement.getPublishedAt())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}
