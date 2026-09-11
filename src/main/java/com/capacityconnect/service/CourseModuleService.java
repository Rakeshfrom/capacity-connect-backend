package com.capacityconnect.service;

import com.capacityconnect.dto.CourseModuleRequest;
import com.capacityconnect.dto.CourseModuleResponse;
import com.capacityconnect.entity.Course;
import com.capacityconnect.entity.CourseModule;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.CourseModuleRepository;
import com.capacityconnect.repository.CourseRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseModuleService {

    private final CourseModuleRepository repository;
    private final CourseRepository courseRepository;
    private final CurrentUserService currentUserService;

    public CourseModuleService(
            CourseModuleRepository repository,
            CourseRepository courseRepository,
            CurrentUserService currentUserService) {
        this.repository = repository;
        this.courseRepository = courseRepository;
        this.currentUserService = currentUserService;
    }

    public List<CourseModuleResponse> getByCourse(Long courseId) {
        return repository.findByCourseIdAndActiveTrueOrderByOrderIndexAsc(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CourseModuleResponse create(
            Long courseId,
            CourseModuleRequest request,
            Authentication authentication) {

        requireCourseAccess(courseId, authentication);

        CourseModule module = new CourseModule();
        module.setCourseId(courseId);
        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());
        module.setOrderIndex(request.getOrderIndex() == null ? 0 : request.getOrderIndex());
        module.setActive(true);

        return toResponse(repository.save(module));
    }

    public CourseModuleResponse update(
            Long id,
            CourseModuleRequest request,
            Authentication authentication) {

        CourseModule module = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course module not found"));

        requireCourseAccess(module.getCourseId(), authentication);

        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());

        if (request.getOrderIndex() != null) {
            module.setOrderIndex(request.getOrderIndex());
        }

        return toResponse(repository.save(module));
    }

    public void delete(Long id, Authentication authentication) {
        CourseModule module = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course module not found"));

        requireCourseAccess(module.getCourseId(), authentication);

        module.setActive(false);
        repository.save(module);
    }

    private void requireCourseAccess(
            Long courseId,
            Authentication authentication) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        User currentUser = currentUserService.getCurrentUser(authentication);

        if (currentUser.getRole() == User.Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() != User.Role.TRAINER
                || !currentUser.getId().equals(course.getTrainerId())) {
            throw new AccessDeniedException(
                    "You do not have permission to manage this course");
        }
    }

    private CourseModuleResponse toResponse(CourseModule module) {
        return new CourseModuleResponse(
                module.getId(),
                module.getCourseId(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                module.getActive()
        );
    }
}
