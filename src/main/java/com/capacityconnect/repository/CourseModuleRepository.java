package com.capacityconnect.repository;

import com.capacityconnect.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    List<CourseModule> findByCourseIdAndActiveTrueOrderByOrderIndexAsc(Long courseId);

    List<CourseModule> findByCourseIdOrderByOrderIndexAsc(Long courseId);
}
