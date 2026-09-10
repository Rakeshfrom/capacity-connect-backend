package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminDashboardResponse {

    private long totalUsers;
    private long trainees;
    private long trainers;
    private long admins;
    private long totalCourses;
    private long publishedCourses;
    private long totalEnrollments;
    private long totalAssessments;
    private List<CourseOverview> courses;

    @Getter
    @Builder
    public static class CourseOverview {
        private Long courseId;
        private String courseTitle;
        private long enrollments;
        private int completion;
    }
}
