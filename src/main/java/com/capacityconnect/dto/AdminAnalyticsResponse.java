package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminAnalyticsResponse {

    private Long totalTrainees;
    private Long activeCourses;
    private Integer averageAssessmentScore;
    private Integer overallCompletion;

    private Integer courseParticipation;
    private Integer assessmentParticipation;
    private Integer resourceEngagement;
    private Integer feedbackResponse;

    private List<CourseAnalytics> courses;

    @Getter
    @Builder
    public static class CourseAnalytics {
        private Long courseId;
        private String courseTitle;
        private Long enrolled;
        private Integer completion;
        private Integer averageScore;
    }
}
