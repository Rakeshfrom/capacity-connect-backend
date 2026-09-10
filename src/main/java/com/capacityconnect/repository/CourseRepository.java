package com.capacityconnect.repository;

import com.capacityconnect.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(Course.Status status);

    boolean existsByTitleIgnoreCase(String title);

    List<Course> findByTrainerId(Long trainerId);
}
