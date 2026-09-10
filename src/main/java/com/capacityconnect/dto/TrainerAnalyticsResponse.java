package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrainerAnalyticsResponse {

    private Long trainerId;
    private Integer totalTrainees;
    private Integer averageCompletion;
    private Integer averageAssessmentScore;
    private Integer overallPerformance;
    private List<CourseAnalytics> courses;

    @Getter
    @Builder
    public static class CourseAnalytics {
        private Long courseId;
        private String courseTitle;
        private Integer trainees;
        private Integer completion;
        private Integer averageScore;
    }
}
