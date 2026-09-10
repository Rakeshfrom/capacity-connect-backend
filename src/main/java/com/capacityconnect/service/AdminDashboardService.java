package com.capacityconnect.service;

import com.capacityconnect.dto.AdminDashboardResponse;
import com.capacityconnect.entity.Assessment;
import com.capacityconnect.entity.Course;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.AssessmentRepository;
import com.capacityconnect.repository.CourseRepository;
import com.capacityconnect.repository.EnrollmentRepository;
import com.capacityconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;

    public AdminDashboardResponse getDashboard() {

        var users = userRepository.findAll();
        var courses = courseRepository.findAll();

        long trainees = users.stream()
                .filter(user -> user.getRole() == User.Role.TRAINEE)
                .count();

        long trainers = users.stream()
                .filter(user -> user.getRole() == User.Role.TRAINER)
                .count();

        long admins = users.stream()
                .filter(user -> user.getRole() == User.Role.ADMIN)
                .count();

        long publishedCourses = courses.stream()
                .filter(course -> course.getStatus() == Course.Status.PUBLISHED)
                .count();

        List<AdminDashboardResponse.CourseOverview> courseOverview = courses.stream()
                .map(course -> {
                    var enrollments = enrollmentRepository.findByCourseId(course.getId());

                    int completion = enrollments.isEmpty()
                            ? 0
                            : (int) Math.round(
                                    enrollments.stream()
                                            .mapToInt(enrollment -> enrollment.getProgress() == null
                                                    ? 0
                                                    : enrollment.getProgress())
                                            .average()
                                            .orElse(0)
                            );

                    return AdminDashboardResponse.CourseOverview.builder()
                            .courseId(course.getId())
                            .courseTitle(course.getTitle())
                            .enrollments(enrollments.size())
                            .completion(completion)
                            .build();
                })
                .toList();

        return AdminDashboardResponse.builder()
                .totalUsers(users.size())
                .trainees(trainees)
                .trainers(trainers)
                .admins(admins)
                .totalCourses(courses.size())
                .publishedCourses(publishedCourses)
                .totalEnrollments(enrollmentRepository.count())
                .totalAssessments(assessmentRepository.count())
                .courses(courseOverview)
                .build();
    }
}
