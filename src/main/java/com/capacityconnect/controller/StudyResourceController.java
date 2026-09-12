package com.capacityconnect.controller;

import com.capacityconnect.dto.StudyResourceResponse;
import com.capacityconnect.entity.StudyResource;
import com.capacityconnect.service.StudyResourceService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/trainee/resources")
@CrossOrigin
public class StudyResourceController {

    private final StudyResourceService service;

    public StudyResourceController(StudyResourceService service) {
        this.service = service;
    }

    @GetMapping
    public List<StudyResourceResponse> getMine(Authentication authentication) {
        return service.getMine(authentication);
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StudyResourceResponse upload(
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam MultipartFile file,
            Authentication authentication) throws Exception {

        return service.createFile(title, description, file, authentication);
    }

    @PostMapping("/link")
    public StudyResourceResponse addLink(
            @RequestBody LinkRequest request,
            Authentication authentication) {

        return service.createLink(
                request.title(),
                request.description(),
                request.url(),
                authentication
        );
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            Authentication authentication) {

        StudyResource resource = service.getMineById(id, authentication);
        Path path = service.getFilePath(id, authentication);

        Resource file = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(
                        resource.getContentType() == null
                                ? MediaType.APPLICATION_OCTET_STREAM
                                : MediaType.parseMediaType(resource.getContentType())
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getOriginalFileName() + "\""
                )
                .body(file);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) throws Exception {

        service.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }

    public record LinkRequest(
            String title,
            String description,
            String url
    ) {}
}
