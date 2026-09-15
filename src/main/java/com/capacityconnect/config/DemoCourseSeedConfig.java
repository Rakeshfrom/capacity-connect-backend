package com.capacityconnect.config;

import com.capacityconnect.entity.Assessment;
import com.capacityconnect.entity.AssessmentAttempt;
import com.capacityconnect.entity.Course;
import com.capacityconnect.entity.CourseModule;
import com.capacityconnect.entity.Enrollment;
import com.capacityconnect.entity.Question;
import com.capacityconnect.entity.User;
import com.capacityconnect.repository.AssessmentAttemptRepository;
import com.capacityconnect.repository.AssessmentRepository;
import com.capacityconnect.repository.CourseModuleRepository;
import com.capacityconnect.repository.CourseRepository;
import com.capacityconnect.repository.EnrollmentRepository;
import com.capacityconnect.repository.QuestionRepository;
import com.capacityconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DemoCourseSeedConfig implements CommandLineRunner {

    private final ApplicationContext applicationContext;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentAttemptRepository attemptRepository;

    @Value("${capacity-connect.demo.seed:false}")
    private boolean seedEnabled;

    private static final String DEMO_CATEGORY = "IMD DEMO";

    private record CourseSeed(
            String title,
            String description,
            String category,
            int durationHours,
            Course.Level level,
            List<ModuleSeed> modules,
            List<QuestionSeed> finalQuestions
    ) {}

    private record ModuleSeed(
            String title,
            String description,
            List<QuestionSeed> quickQuestions
    ) {}

    private record QuestionSeed(
            String text,
            String a,
            String b,
            String c,
            String d,
            Question.CorrectOption correct,
            int marks
    ) {}

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        System.out.println("=== CAPACITY CONNECT DEMO COURSE SEED ===");

        User trainer = userRepository.findAll().stream()
                .filter(user -> "trainer1".equalsIgnoreCase(user.getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "trainer1 user was not found in the database"));

        if (trainer.getRole() != User.Role.TRAINER) {
            throw new IllegalStateException(
                    "trainer1 exists but is not a TRAINER. Current role: " + trainer.getRole());
        }

        List<User> trainees = userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.Role.TRAINEE)
                .filter(user -> user.getStatus() == User.Status.ACTIVE)
                .sorted(Comparator.comparing(User::getId))
                .limit(6)
                .toList();

        if (trainees.isEmpty()) {
            throw new IllegalStateException(
                    "No ACTIVE trainee users found. Create at least one trainee first.");
        }

        List<CourseSeed> seeds = buildSeeds();
        int courseCount = 0;
        int enrollmentCount = 0;
        int attemptCount = 0;

        for (CourseSeed seed : seeds) {
            Course course = upsertCourse(seed, trainer.getId());
            courseCount++;

            List<CourseModule> modules = upsertModules(seed, course.getId());

            List<Assessment> assessments = new ArrayList<>();
            for (ModuleSeed moduleSeed : seed.modules()) {
                CourseModule module = modules.stream()
                        .filter(item -> item.getTitle().equals(moduleSeed.title()))
                        .findFirst()
                        .orElseThrow();

                Assessment quick = upsertAssessment(
                        "Quick Quiz · " + moduleSeed.title(),
                        "Practice check for " + moduleSeed.title(),
                        course.getId(),
                        module.getId(),
                        10,
                        60
                );
                upsertQuestions(quick, moduleSeed.quickQuestions());
                assessments.add(quick);
            }

            Assessment finalAssessment = upsertAssessment(
                    "Final Assessment · " + seed.title(),
                    "Final assessment for the IMD learning programme.",
                    course.getId(),
                    null,
                    30,
                    60
            );
            upsertQuestions(finalAssessment, seed.finalQuestions());
            assessments.add(finalAssessment);

            for (int i = 0; i < trainees.size(); i++) {
                User trainee = trainees.get(i);

                int progress = progressFor(i, courseCount - 1);
                Enrollment enrollment = upsertEnrollment(
                        trainee.getId(),
                        course.getId(),
                        progress
                );
                enrollmentCount++;

                for (Assessment assessment : assessments) {
                    int percentage = attemptPercentage(i, courseCount - 1, assessment == finalAssessment);
                    upsertAttempt(
                            assessment,
                            trainee.getId(),
                            percentage
                    );
                    attemptCount++;
                }
            }
        }

        System.out.printf(
                "Seeded %d courses for trainer1, %d trainee-course enrollments, %d assessment attempts.%n",
                courseCount, enrollmentCount, attemptCount
        );
        System.out.println("Trainer username: trainer1");

        SpringApplication.exit(applicationContext, () -> 0);
    }

    private Course upsertCourse(CourseSeed seed, Long trainerId) {
        Course course = courseRepository.findAll().stream()
                .filter(item -> seed.title().equalsIgnoreCase(item.getTitle()))
                .filter(item -> DEMO_CATEGORY.equalsIgnoreCase(
                        item.getCategory() == null ? "" : item.getCategory()
                ))
                .findFirst()
                .orElseGet(Course::new);

        course.setTitle(seed.title());
        course.setDescription(seed.description());
        course.setCategory(DEMO_CATEGORY);
        course.setDurationHours(seed.durationHours());
        course.setLevel(seed.level());
        course.setStatus(Course.Status.PUBLISHED);
        course.setTrainerId(trainerId);

        return courseRepository.save(course);
    }

    private List<CourseModule> upsertModules(CourseSeed seed, Long courseId) {
        List<CourseModule> result = new ArrayList<>();

        for (int i = 0; i < seed.modules().size(); i++) {
            ModuleSeed seedModule = seed.modules().get(i);

            CourseModule module = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                    .filter(item -> seedModule.title().equalsIgnoreCase(item.getTitle()))
                    .findFirst()
                    .orElseGet(CourseModule::new);

            module.setCourseId(courseId);
            module.setTitle(seedModule.title());
            module.setDescription(seedModule.description());
            module.setOrderIndex(i + 1);
            module.setActive(true);

            result.add(moduleRepository.save(module));
        }

        return result;
    }

    private Assessment upsertAssessment(
            String title,
            String description,
            Long courseId,
            Long moduleId,
            int minutes,
            int passing
    ) {
        Assessment assessment = assessmentRepository.findByCourseId(courseId).stream()
                .filter(item -> title.equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .orElseGet(Assessment::new);

        assessment.setTitle(title);
        assessment.setDescription(description);
        assessment.setCourseId(courseId);
        assessment.setModuleId(moduleId);
        assessment.setTimeLimitMinutes(minutes);
        assessment.setPassingPercentage(passing);
        assessment.setStatus(Assessment.Status.PUBLISHED);

        return assessmentRepository.save(assessment);
    }

    private void upsertQuestions(Assessment assessment, List<QuestionSeed> seeds) {
        List<Question> existing = questionRepository.findByAssessmentId(assessment.getId());

        if (existing.size() == seeds.size()) {
            for (int i = 0; i < seeds.size(); i++) {
                Question question = existing.get(i);
                applyQuestion(question, assessment.getId(), seeds.get(i));
                questionRepository.save(question);
            }
            return;
        }

        for (Question old : existing) {
            questionRepository.delete(old);
        }

        for (QuestionSeed seed : seeds) {
            Question question = Question.builder()
                    .assessmentId(assessment.getId())
                    .questionText(seed.text())
                    .optionA(seed.a())
                    .optionB(seed.b())
                    .optionC(seed.c())
                    .optionD(seed.d())
                    .correctOption(seed.correct())
                    .marks(seed.marks())
                    .build();

            questionRepository.save(question);
        }
    }

    private void applyQuestion(
            Question question,
            Long assessmentId,
            QuestionSeed seed
    ) {
        question.setAssessmentId(assessmentId);
        question.setQuestionText(seed.text());
        question.setOptionA(seed.a());
        question.setOptionB(seed.b());
        question.setOptionC(seed.c());
        question.setOptionD(seed.d());
        question.setCorrectOption(seed.correct());
        question.setMarks(seed.marks());
    }

    private Enrollment upsertEnrollment(
            Long traineeId,
            Long courseId,
            int progress
    ) {
        Enrollment enrollment = enrollmentRepository
                .findByTraineeIdAndCourseId(traineeId, courseId)
                .orElseGet(Enrollment::new);

        enrollment.setTraineeId(traineeId);
        enrollment.setCourseId(courseId);

        int safeProgress = Math.max(0, Math.min(100, progress));
        enrollment.setProgress(safeProgress);

        if (safeProgress == 100) {
            enrollment.setStatus(Enrollment.Status.COMPLETED);
            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(LocalDateTime.now().minusDays(1));
            }
        } else if (safeProgress > 0) {
            enrollment.setStatus(Enrollment.Status.IN_PROGRESS);
            enrollment.setCompletedAt(null);
        } else {
            enrollment.setStatus(Enrollment.Status.ENROLLED);
            enrollment.setCompletedAt(null);
        }

        return enrollmentRepository.save(enrollment);
    }

    private void upsertAttempt(
            Assessment assessment,
            Long traineeId,
            int percentage
    ) {
        int totalMarks = questionRepository.findByAssessmentId(assessment.getId()).stream()
                .mapToInt(Question::getMarks)
                .sum();

        int safePercentage = Math.max(0, Math.min(100, percentage));
        int score = Math.round(totalMarks * (safePercentage / 100f));

        AssessmentAttempt attempt = attemptRepository
                .findByAssessmentIdAndTraineeId(assessment.getId(), traineeId)
                .stream()
                .findFirst()
                .orElseGet(AssessmentAttempt::new);

        attempt.setAssessmentId(assessment.getId());
        attempt.setTraineeId(traineeId);
        attempt.setScore(score);
        attempt.setTotalMarks(totalMarks);
        attempt.setPercentage(safePercentage);
        attempt.setResult(
                safePercentage >= assessment.getPassingPercentage()
                        ? AssessmentAttempt.Result.PASSED
                        : AssessmentAttempt.Result.FAILED
        );
        if (attempt.getStartedAt() == null) {
            attempt.setStartedAt(LocalDateTime.now().minusDays(3));
        }
        attempt.setSubmittedAt(LocalDateTime.now().minusDays(2));

        attemptRepository.save(attempt);
    }

    private int progressFor(int traineeIndex, int courseIndex) {
        int[][] values = {
                {82, 74, 91, 58, 67, 43},
                {76, 88, 69, 61, 79, 52},
                {93, 81, 72, 64, 55, 47},
                {68, 90, 77, 59, 71, 45}
        };

        return values[Math.max(0, Math.min(values.length - 1, courseIndex))]
                [Math.max(0, Math.min(values[courseIndex].length - 1, traineeIndex))];
    }

    private int attemptPercentage(int traineeIndex, int courseIndex, boolean finalAssessment) {
        int[][] values = {
                {88, 81, 94, 69, 76, 62},
                {84, 91, 78, 72, 86, 58},
                {95, 87, 79, 74, 63, 55},
                {78, 93, 82, 68, 75, 57}
        };

        int base = values[Math.max(0, Math.min(values.length - 1, courseIndex))]
                [Math.max(0, Math.min(values[courseIndex].length - 1, traineeIndex))];

        return finalAssessment ? base : Math.min(100, base + 3);
    }

    private List<CourseSeed> buildSeeds() {
        QuestionSeed q1 = new QuestionSeed(
                "Which atmospheric variable is most directly measured by a thermometer?",
                "Pressure", "Temperature", "Humidity", "Wind speed",
                Question.CorrectOption.B, 2
        );
        QuestionSeed q2 = new QuestionSeed(
                "What is the main purpose of surface meteorological observations?",
                "To monitor weather conditions", "To issue passports", "To map roads", "To measure earthquakes",
                Question.CorrectOption.A, 2
        );
        QuestionSeed q3 = new QuestionSeed(
                "Which instrument is commonly used to measure air pressure?",
                "Rain gauge", "Barometer", "Anemometer", "Hygrometer",
                Question.CorrectOption.B, 2
        );
        QuestionSeed q4 = new QuestionSeed(
                "What does an anemometer measure?",
                "Wind speed", "Rainfall", "Air temperature", "Visibility",
                Question.CorrectOption.A, 2
        );
        QuestionSeed q5 = new QuestionSeed(
                "Which observation is important for aviation weather monitoring?",
                "Ceiling and visibility", "Soil colour", "Building height", "River width",
                Question.CorrectOption.A, 2
        );

        List<QuestionSeed> finalObservation = List.of(q1, q2, q3, q4, q5);

        List<QuestionSeed> quickObservation = List.of(
                new QuestionSeed("A thermometer measures:", "Pressure", "Temperature", "Wind", "Rainfall", Question.CorrectOption.B, 1),
                new QuestionSeed("A barometer measures:", "Humidity", "Pressure", "Visibility", "Cloud type", Question.CorrectOption.B, 1),
                new QuestionSeed("An anemometer measures:", "Wind speed", "Temperature", "Pressure", "Rainfall", Question.CorrectOption.A, 1)
        );

        List<CourseSeed> seeds = new ArrayList<>();
        seeds.add(new CourseSeed(
                "Meteorological Observation & Instruments",
                "Structured IMD learning on observation systems, instruments and operational weather measurements.",
                DEMO_CATEGORY,
                24,
                Course.Level.BEGINNER,
                List.of(
                        new ModuleSeed(
                                "Observation Fundamentals",
                                "Core concepts of meteorological observations and operational instrumentation.",
                                quickObservation
                        ),
                        new ModuleSeed(
                                "Instrument Handling & Quality Checks",
                                "Instrument principles, field practice and observation quality control.",
                                quickObservation
                        )
                ),
                finalObservation
        ));

        List<QuestionSeed> forecastQuestions = List.of(
                new QuestionSeed("What is the primary purpose of a weather forecast?", "To describe likely future atmospheric conditions", "To record history only", "To measure earthquakes", "To map buildings", Question.CorrectOption.A, 2),
                new QuestionSeed("Which field is central to numerical weather prediction?", "Atmospheric state variables", "Traffic density", "Soil color", "Population count", Question.CorrectOption.A, 2),
                new QuestionSeed("Forecast verification compares forecasts with:", "Observed outcomes", "Building plans", "Ocean depth only", "Satellite launch dates", Question.CorrectOption.A, 2),
                new QuestionSeed("A nowcast generally focuses on:", "Very short-range weather", "Decadal climate only", "Historical archives", "Astronomical events", Question.CorrectOption.A, 2),
                new QuestionSeed("Forecast uncertainty generally increases with:", "Forecast lead time", "Observation frequency", "Data quality", "Model resolution", Question.CorrectOption.A, 2)
        );

        List<QuestionSeed> quickForecast = List.of(
                new QuestionSeed("Forecast verification uses:", "Observed outcomes", "Guesswork", "Maps only", "Climate normals only", Question.CorrectOption.A, 1),
                new QuestionSeed("Nowcasting targets:", "Very short-range weather", "Long-term climate", "Historical weather", "Ocean tides", Question.CorrectOption.A, 1),
                new QuestionSeed("NWP stands for:", "Numerical Weather Prediction", "National Wind Programme", "New Weather Portal", "Network Warning Protocol", Question.CorrectOption.A, 1)
        );

        seeds.add(new CourseSeed(
                "Weather Forecasting Fundamentals",
                "Foundational forecasting workflow covering observations, numerical guidance and forecast verification.",
                DEMO_CATEGORY,
                28,
                Course.Level.INTERMEDIATE,
                List.of(
                        new ModuleSeed("Forecasting Workflow", "From observations to operational forecast products.", quickForecast),
                        new ModuleSeed("Forecast Verification", "Interpret forecast accuracy, uncertainty and verification practices.", quickForecast)
                ),
                forecastQuestions
        ));

        List<QuestionSeed> satelliteQuestions = List.of(
                new QuestionSeed("Satellite meteorology is especially useful for:", "Large-area cloud and weather monitoring", "Measuring classroom attendance", "Road construction", "Soil ownership", Question.CorrectOption.A, 2),
                new QuestionSeed("Geostationary satellites are valuable for:", "Frequent monitoring of a fixed region", "Daily ground surveys only", "Deep ocean drilling", "Earthquake excavation", Question.CorrectOption.A, 2),
                new QuestionSeed("Remote sensing estimates atmospheric properties using:", "Radiation measurements", "Paper maps", "Road sensors", "Building sensors", Question.CorrectOption.A, 2),
                new QuestionSeed("Water vapour imagery can help identify:", "Moisture patterns", "Road speed", "Soil ownership", "Population density", Question.CorrectOption.A, 2),
                new QuestionSeed("Satellite imagery can support monitoring of:", "Cloud systems and severe weather", "School timetables", "Building permits", "Vehicle registration", Question.CorrectOption.A, 2)
        );

        List<QuestionSeed> quickSatellite = List.of(
                new QuestionSeed("Geostationary satellites provide:", "Frequent regional views", "One image per decade", "Only ground photos", "Only ocean soundings", Question.CorrectOption.A, 1),
                new QuestionSeed("Remote sensing relies on:", "Radiation measurements", "Road counters", "Building plans", "Manual typing", Question.CorrectOption.A, 1),
                new QuestionSeed("Water vapour imagery highlights:", "Moisture patterns", "Traffic signals", "Land ownership", "Population counts", Question.CorrectOption.A, 1)
        );

        seeds.add(new CourseSeed(
                "Satellite Meteorology & Remote Sensing",
                "Operational satellite interpretation, remote sensing concepts and weather monitoring applications.",
                DEMO_CATEGORY,
                26,
                Course.Level.INTERMEDIATE,
                List.of(
                        new ModuleSeed("Satellite Observation", "Satellite platforms, imagery and operational observation concepts.", quickSatellite),
                        new ModuleSeed("Remote Sensing Applications", "Interpret satellite-derived signals for weather analysis.", quickSatellite)
                ),
                satelliteQuestions
        ));

        List<QuestionSeed> monsoonQuestions = List.of(
                new QuestionSeed("Monsoon analysis commonly considers:", "Seasonal circulation and rainfall patterns", "Building height only", "Road traffic only", "Population count only", Question.CorrectOption.A, 2),
                new QuestionSeed("An extreme rainfall event is best assessed using:", "Observed rainfall and atmospheric context", "A single photograph", "Road maps only", "Population data only", Question.CorrectOption.A, 2),
                new QuestionSeed("Heavy rainfall nowcasting benefits from:", "High-frequency observations", "Annual reports only", "Static maps only", "Decade-old data only", Question.CorrectOption.A, 2),
                new QuestionSeed("Flood risk is influenced by:", "Rainfall intensity and catchment conditions", "Building colour", "Latitude alone", "Road signs", Question.CorrectOption.A, 2),
                new QuestionSeed("Operational extreme-weather analysis supports:", "Early warning and preparedness", "Passport processing", "Road tolling", "Tax filing", Question.CorrectOption.A, 2)
        );

        List<QuestionSeed> quickMonsoon = List.of(
                new QuestionSeed("Monsoon analysis focuses on:", "Seasonal circulation and rainfall", "Road traffic", "Building permits", "Population only", Question.CorrectOption.A, 1),
                new QuestionSeed("Extreme rainfall analysis needs:", "Observed rainfall", "Road maps only", "Population counts only", "Building color", Question.CorrectOption.A, 1),
                new QuestionSeed("Nowcasting benefits from:", "High-frequency observations", "Annual reports only", "Old maps only", "Static photographs", Question.CorrectOption.A, 1)
        );

        seeds.add(new CourseSeed(
                "Monsoon & Extreme Weather Analysis",
                "Applied learning on monsoon behaviour, extreme rainfall and operational severe-weather analysis.",
                DEMO_CATEGORY,
                30,
                Course.Level.ADVANCED,
                List.of(
                        new ModuleSeed("Monsoon Diagnostics", "Seasonal circulation, rainfall patterns and diagnostic indicators.", quickMonsoon),
                        new ModuleSeed("Extreme Weather Analysis", "Operational interpretation of severe rainfall and related hazards.", quickMonsoon)
                ),
                monsoonQuestions
        ));

        return seeds;
    }
}
