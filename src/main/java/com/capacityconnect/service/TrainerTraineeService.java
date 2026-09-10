package com.capacityconnect.service;

import com.capacityconnect.dto.TrainerTraineeResponse;
import com.capacityconnect.entity.Course;
import com.capacityconnect.entity.Enrollment;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.CourseRepository;
import com.capacityconnect.repository.EnrollmentRepository;
import com.capacityconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainerTraineeService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public List<TrainerTraineeResponse> getTraineesByTrainer(Long trainerId) {
        List<Course> courses = courseRepository.findByTrainerId(trainerId);

        return courses.stream()
                .flatMap(course -> enrollmentRepository.findByCourseId(course.getId()).stream())
                .map(enrollment -> buildResponse(enrollment, courses))
                .toList();
    }

    private TrainerTraineeResponse buildResponse(
            Enrollment enrollment,
            List<Course> courses
    ) {
        User trainee = userRepository.findById(enrollment.getTraineeId())
                .orElseThrow(() -> new IllegalArgumentException("Trainee not found"));

        Course course = courses.stream()
                .filter(c -> c.getId().equals(enrollment.getCourseId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        return TrainerTraineeResponse.builder()
                .traineeId(trainee.getId())
                .username(trainee.getUsername())
                .firstName(trainee.getFirstName())
                .lastName(trainee.getLastName())
                .email(trainee.getEmail())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .enrollmentStatus(enrollment.getStatus().name())
                .progress(enrollment.getProgress())
                .build();
    }
}
