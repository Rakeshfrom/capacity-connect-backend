package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrainerTraineeDetailResponse {

    private Long traineeId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private List<CoursePerformance> courses;

    @Getter
    @Builder
    public static class CoursePerformance {
        private Long courseId;
        private String courseTitle;
        private String enrollmentStatus;
        private Integer progress;
        private List<AssessmentPerformance> assessments;
    }

    @Getter
    @Builder
    public static class AssessmentPerformance {
        private Long assessmentId;
        private String assessmentTitle;
        private Integer percentage;
        private String result;
    }
}
