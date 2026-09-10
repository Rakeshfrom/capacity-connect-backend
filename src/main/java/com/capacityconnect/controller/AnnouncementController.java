package com.capacityconnect.controller;

import com.capacityconnect.dto.AnnouncementRequest;
import com.capacityconnect.dto.AnnouncementResponse;
import com.capacityconnect.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public List<AnnouncementResponse> getAll() {
        return announcementService.getAll();
    }

    @GetMapping("/published")
    public List<AnnouncementResponse> getPublished() {
        return announcementService.getPublished();
    }

    @GetMapping("/{id}")
    public AnnouncementResponse getById(@PathVariable Long id) {
        return announcementService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return announcementService.create(request);
    }

    @PutMapping("/{id}")
    public AnnouncementResponse update(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return announcementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        announcementService.delete(id);
    }
}
