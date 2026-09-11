package com.capacityconnect.controller;

import com.capacityconnect.dto.CourseModuleRequest;
import com.capacityconnect.dto.CourseModuleResponse;
import com.capacityconnect.service.CourseModuleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course-modules")
public class CourseModuleController {

    private final CourseModuleService moduleService;

    public CourseModuleController(CourseModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping("/course/{courseId}")
    public List<CourseModuleResponse> getByCourse(
            @PathVariable Long courseId) {
        return moduleService.getByCourse(courseId);
    }

    @PostMapping("/course/{courseId}")
    public CourseModuleResponse create(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseModuleRequest request,
            Authentication authentication) {

        return moduleService.create(courseId, request, authentication);
    }

    @PutMapping("/{id}")
    public CourseModuleResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CourseModuleRequest request,
            Authentication authentication) {

        return moduleService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {

        moduleService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
