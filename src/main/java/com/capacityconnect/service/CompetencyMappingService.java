package com.capacityconnect.service;

import com.capacityconnect.dto.CompetencyMappingResponse;
import com.capacityconnect.entity.Course;
import com.capacityconnect.entity.TrainerProfile;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.CourseRepository;
import com.capacityconnect.repository.TrainerProfileRepository;
import com.capacityconnect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CompetencyMappingService {

    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final CourseRepository courseRepository;

    public CompetencyMappingService(
            UserRepository userRepository,
            TrainerProfileRepository trainerProfileRepository,
            CourseRepository courseRepository
    ) {
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.courseRepository = courseRepository;
    }

    public CompetencyMappingResponse findSuitableTrainers(String competency) {
        String required = competency == null ? "" : competency.trim().toLowerCase(Locale.ROOT);

        List<CompetencyMappingResponse.TrainerMatch> matches =
                userRepository.findAll().stream()
                        .filter(user -> user.getRole() == User.Role.TRAINER)
                        .filter(user -> user.getStatus() == User.Status.ACTIVE)
                        .map(user -> buildMatch(user, required))
                        .sorted(Comparator.comparing(
                                CompetencyMappingResponse.TrainerMatch::getScore
                        ).reversed())
                        .toList();

        return CompetencyMappingResponse.builder()
                .competency(competency)
                .trainers(matches)
                .build();
    }

    private CompetencyMappingResponse.TrainerMatch buildMatch(
            User user,
            String competency
    ) {
        TrainerProfile profile = trainerProfileRepository
                .findByTrainerId(user.getId())
                .orElse(null);

        String expertise = profile == null ? "" : nullToEmpty(profile.getExpertise());
        String specialization = profile == null
                ? ""
                : nullToEmpty(profile.getSpecialization());

        String searchable = (expertise + " " + specialization)
                .toLowerCase(Locale.ROOT);

        int competencyScore = searchable.contains(competency) ? 60 : 0;

        int experienceYears = profile == null || profile.getExperienceYears() == null
                ? 0
                : profile.getExperienceYears();

        int experienceScore = Math.min(25, experienceYears * 2);

        long assignedCourses = courseRepository.findAll().stream()
                .filter(course -> user.getId().equals(course.getTrainerId()))
                .filter(course -> course.getStatus() == Course.Status.PUBLISHED)
                .count();

        String workload;
        int workloadScore;

        if (assignedCourses <= 1) {
            workload = "Low";
            workloadScore = 15;
        } else if (assignedCourses <= 3) {
            workload = "Medium";
            workloadScore = 10;
        } else {
            workload = "High";
            workloadScore = 5;
        }

        return CompetencyMappingResponse.TrainerMatch.builder()
                .trainerId(user.getId())
                .name((user.getFirstName() + " " +
                        nullToEmpty(user.getLastName())).trim())
                .department(profile != null && profile.getDepartment() != null
                        ? profile.getDepartment()
                        : user.getDepartment())
                .expertise(expertise.isBlank() ? specialization : expertise)
                .experienceYears(experienceYears)
                .workload(workload)
                .score(competencyScore + experienceScore + workloadScore)
                .build();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
