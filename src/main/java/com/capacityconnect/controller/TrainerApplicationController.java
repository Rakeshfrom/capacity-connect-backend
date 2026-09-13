package com.capacityconnect.controller;

import com.capacityconnect.dto.TrainerApplicationRequest;
import com.capacityconnect.dto.TrainerApplicationResponse;
import com.capacityconnect.dto.TrainerApplicationReviewRequest;
import com.capacityconnect.dto.TrainerAssessmentResponse;
import com.capacityconnect.dto.TrainerAssessmentSubmissionRequest;
import com.capacityconnect.service.TrainerApplicationService;
import com.capacityconnect.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer-applications")
public class TrainerApplicationController {

    private final TrainerApplicationService service;
    private final FileStorageService fileStorageService;

    public TrainerApplicationController(
            TrainerApplicationService service,
            FileStorageService fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TrainerApplicationResponse submit(
            Authentication authentication,
            @RequestPart("data") @Valid TrainerApplicationRequest request,
            @RequestPart(value = "supportingDocument", required = false)
            org.springframework.web.multipart.MultipartFile supportingDocument) {

        return service.submit(authentication, request, supportingDocument);
    }

    @GetMapping("/me/assessment")
    public TrainerAssessmentResponse getMyAssessment(Authentication authentication) {
        return service.getMyAssessment(authentication);
    }

    @PostMapping("/me/assessment")
    public TrainerAssessmentResponse submitMyAssessment(Authentication authentication, @RequestBody TrainerAssessmentSubmissionRequest request) {
        return service.submitMyAssessment(authentication, request);
    }

    @GetMapping("/me")
    public TrainerApplicationResponse getMyApplication(
            Authentication authentication) {

        return service.getMyApplication(authentication);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<TrainerApplicationResponse> getAllApplications() {
        return service.getAllApplications();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TrainerApplicationResponse> getPendingApplications() {
        return service.getPendingApplications();
    }

    @GetMapping("/{id}/document")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long id) {

        TrainerApplicationResponse application = service.getApplication(id);

        if (application.getSupportingDocumentKey() == null
                || application.getSupportingDocumentKey().isBlank()) {
            throw new IllegalArgumentException(
                    "No uploaded document found");
        }

        var path = fileStorageService.load(
                application.getSupportingDocumentKey());

        Resource file = new FileSystemResource(path);

        MediaType mediaType = MediaTypeFactory
                .getMediaType(path.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + path.getFileName() + "\""
                )
                .body(file);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TrainerApplicationResponse getApplication(
            @PathVariable Long id) {
        return service.getApplication(id);
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public TrainerApplicationResponse reviewApplication(
            @PathVariable Long id,
            @Valid @RequestBody TrainerApplicationReviewRequest request) {

        return service.reviewApplication(id, request);
    }
}
