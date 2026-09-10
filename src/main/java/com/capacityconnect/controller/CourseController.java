package com.capacityconnect.controller;

import com.capacityconnect.dto.CourseRequest;
import com.capacityconnect.dto.CourseResponse;
import com.capacityconnect.entity.Course;
import com.capacityconnect.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseResponse> getAllCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/{id}")
    public CourseResponse getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @GetMapping("/status/{status}")
    public List<CourseResponse> getCoursesByStatus(
            @PathVariable Course.Status status) {
        return courseService.getCoursesByStatus(status);
    }

    @GetMapping("/trainer/{trainerId}")
    public List<CourseResponse> getCoursesByTrainer(
            @PathVariable Long trainerId) {
        return courseService.getCoursesByTrainer(trainerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse createCourse(
            @Valid @RequestBody CourseRequest request,
            Authentication authentication) {
        return courseService.createCourse(request, authentication);
    }

    @PutMapping("/{id}")
    public CourseResponse updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request,
            Authentication authentication) {
        return courseService.updateCourse(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            @PathVariable Long id,
            Authentication authentication) {
        courseService.deleteCourse(id, authentication);
    }
}
