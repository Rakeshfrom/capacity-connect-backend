package com.capacityconnect.service;

import com.capacityconnect.dto.CourseRequest;
import com.capacityconnect.dto.CourseResponse;
import com.capacityconnect.entity.Course;
import com.capacityconnect.exception.DuplicateResourceException;
import com.capacityconnect.exception.ResourceNotFoundException;
import com.capacityconnect.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public CourseService(
            CourseRepository courseRepository,
            AuditLogService auditLogService,
            CurrentUserService currentUserService) {
        this.courseRepository = courseRepository;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CourseResponse> getCoursesByStatus(Course.Status status) {
        return courseRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CourseResponse> getCoursesByTrainer(Long trainerId) {
        return courseRepository.findByTrainerId(trainerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CourseResponse getCourseById(Long id) {
        return toResponse(findCourse(id));
    }

    public CourseResponse createCourse(
            CourseRequest request,
            org.springframework.security.core.Authentication authentication) {
        if (courseRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new DuplicateResourceException(
                    "Course title already exists: " + request.getTitle());
        }

        com.capacityconnect.entity.User currentUser =
                currentUserService.getCurrentUser(authentication);

        if (currentUser.getRole() != com.capacityconnect.entity.User.Role.TRAINER) {
            throw new IllegalStateException("Only trainers can create courses");
        }

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .durationHours(request.getDurationHours())
                .level(request.getLevel())
                .status(request.getStatus())
                .trainerId(currentUser.getId())
                .build();

        Course savedCourse = courseRepository.save(course);

        auditLogService.create(
                null,
                "System",
                "Course created",
                "Course Management",
                savedCourse.getTitle(),
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );

        return toResponse(savedCourse);
    }

    public CourseResponse updateCourse(
            Long id,
            CourseRequest request,
            org.springframework.security.core.Authentication authentication) {

        Course course = findCourse(id);
        com.capacityconnect.entity.User currentUser =
                currentUserService.getCurrentUser(authentication);

        if (currentUser.getRole() == com.capacityconnect.entity.User.Role.TRAINER
                && !currentUser.getId().equals(course.getTrainerId())) {
            throw new IllegalStateException(
                    "Trainer can update only their own courses");
        }

        courseRepository.findAll()
                .stream()
                .filter(existing -> !existing.getId().equals(id))
                .filter(existing -> existing.getTitle().equalsIgnoreCase(request.getTitle()))
                .findFirst()
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Course title already exists: " + request.getTitle());
                });

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setDurationHours(request.getDurationHours());
        course.setLevel(request.getLevel());
        course.setStatus(request.getStatus());

        if (currentUser.getRole() == com.capacityconnect.entity.User.Role.ADMIN) {
            course.setTrainerId(request.getTrainerId());
        }

        Course updatedCourse = courseRepository.save(course);

        auditLogService.create(
                null,
                "System",
                "Course updated",
                "Course Management",
                updatedCourse.getTitle(),
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );

        return toResponse(updatedCourse);
    }

    public void deleteCourse(
            Long id,
            org.springframework.security.core.Authentication authentication) {

        Course course = findCourse(id);
        com.capacityconnect.entity.User currentUser =
                currentUserService.getCurrentUser(authentication);

        if (currentUser.getRole() == com.capacityconnect.entity.User.Role.TRAINER
                && !currentUser.getId().equals(course.getTrainerId())) {
            throw new IllegalStateException(
                    "Trainer can delete only their own courses");
        }
        String target = course.getTitle();

        courseRepository.delete(course);

        auditLogService.create(
                null,
                "System",
                "Course deleted",
                "Course Management",
                target,
                com.capacityconnect.entity.AuditLog.Status.COMPLETED
        );
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Course not found with id: " + id));
    }


    public List<CourseResponse> getByTrainer(Long trainerId) {
        return courseRepository.findByTrainerId(trainerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .category(course.getCategory())
                .durationHours(course.getDurationHours())
                .level(course.getLevel())
                .status(course.getStatus())
                .trainerId(course.getTrainerId())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
