package com.capacityconnect.service;

import com.capacityconnect.dto.LearningSessionStartRequest;
import com.capacityconnect.entity.CourseModule;
import com.capacityconnect.entity.Enrollment;
import com.capacityconnect.entity.LearningSession;
import com.capacityconnect.repository.CourseModuleRepository;
import com.capacityconnect.repository.EnrollmentRepository;
import com.capacityconnect.repository.LearningSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningSessionService {

    private static final long STALE_SECONDS = 120;

    private final LearningSessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseModuleRepository moduleRepository;

    public LearningSessionService(
            LearningSessionRepository sessionRepository,
            EnrollmentRepository enrollmentRepository,
            CourseModuleRepository moduleRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleRepository = moduleRepository;
    }

    @Transactional
    public LearningSession start(Long traineeId, LearningSessionStartRequest request) {
        if (request == null || request.courseId() == null || request.moduleId() == null) {
            throw new IllegalArgumentException("Course and module are required");
        }

        enrollmentRepository
                .findByTraineeIdAndCourseId(traineeId, request.courseId())
                .orElseThrow(() -> new IllegalStateException("Trainee is not enrolled in this course"));

        CourseModule module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new IllegalStateException("Module not found"));

        if (!Objects.equals(module.getCourseId(), request.courseId())) {
            throw new IllegalStateException("Module does not belong to this course");
        }

        closeStale(traineeId);

        LocalDateTime now = LocalDateTime.now();

        return sessionRepository.save(
                LearningSession.builder()
                        .traineeId(traineeId)
                        .courseId(request.courseId())
                        .moduleId(request.moduleId())
                        .startedAt(now)
                        .lastHeartbeatAt(now)
                        .durationSeconds(0L)
                        .active(true)
                        .build()
        );
    }

    @Transactional
    public LearningSession heartbeat(Long traineeId, Long sessionId) {
        LearningSession session = ownedSession(traineeId, sessionId);

        if (!Boolean.TRUE.equals(session.getActive())) {
            return session;
        }

        LocalDateTime now = LocalDateTime.now();
        addElapsed(session, now);

        session.setLastHeartbeatAt(now);

        return sessionRepository.save(session);
    }

    @Transactional
    public LearningSession stop(Long traineeId, Long sessionId) {
        LearningSession session = ownedSession(traineeId, sessionId);

        if (!Boolean.TRUE.equals(session.getActive())) {
            return session;
        }

        LocalDateTime now = LocalDateTime.now();
        addElapsed(session, now);

        session.setLastHeartbeatAt(now);
        session.setEndedAt(now);
        session.setActive(false);

        return sessionRepository.save(session);
    }

    public Map<String, Object> getMySummary(Long traineeId) {
        closeStale(traineeId);

        List<LearningSession> sessions =
                sessionRepository.findByTraineeIdOrderByStartedAtDesc(traineeId);

        Map<Long, CourseModule> modules = new HashMap<>();
        for (LearningSession session : sessions) {
            moduleRepository.findById(session.getModuleId())
                    .ifPresent(module -> modules.put(module.getId(), module));
        }

        Map<String, Map<String, Object>> moduleAgg = new LinkedHashMap<>();

        for (LearningSession session : sessions) {
            CourseModule module = modules.get(session.getModuleId());
            String moduleTitle = module != null ? module.getTitle() : "Module #" + session.getModuleId();

            String key = session.getCourseId() + ":" + session.getModuleId();

            Map<String, Object> row = moduleAgg.computeIfAbsent(key, k -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("courseId", session.getCourseId());
                value.put("moduleId", session.getModuleId());
                value.put("moduleTitle", moduleTitle);
                value.put("minutes", 0L);
                value.put("sessions", 0L);
                value.put("lastActivity", null);
                return value;
            });

            long oldMinutes = ((Number) row.get("minutes")).longValue();
            row.put("minutes", oldMinutes + (session.getDurationSeconds() / 60));

            long oldSessions = ((Number) row.get("sessions")).longValue();
            row.put("sessions", oldSessions + 1);

            if (row.get("lastActivity") == null ||
                    session.getStartedAt().isAfter((LocalDateTime) row.get("lastActivity"))) {
                row.put("lastActivity", session.getStartedAt());
            }
        }

        List<Map<String, Object>> moduleRows = new ArrayList<>(moduleAgg.values());

        List<Map<String, Object>> recent = sessions.stream()
                .limit(10)
                .map(session -> {
                    CourseModule module = modules.get(session.getModuleId());

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseId", session.getCourseId());
                    row.put("moduleId", session.getModuleId());
                    row.put(
                            "moduleTitle",
                            module != null ? module.getTitle() : "Module #" + session.getModuleId()
                    );
                    row.put("minutes", session.getDurationSeconds() / 60);
                    row.put("startedAt", session.getStartedAt());
                    row.put("endedAt", session.getEndedAt());
                    row.put("active", session.getActive());
                    return row;
                })
                .toList();

        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> day = bucket(
                sessions,
                now.minusDays(13),
                now,
                "day"
        );

        YearMonth currentMonth = YearMonth.from(now);
        LocalDateTime monthFrom =
                currentMonth.minusMonths(11).atDay(1).atStartOfDay();

        List<Map<String, Object>> month = bucket(
                sessions,
                monthFrom,
                now,
                "month"
        );

        LocalDateTime yearFrom =
                LocalDate.of(now.getYear() - 4, 1, 1).atStartOfDay();

        List<Map<String, Object>> year = bucket(
                sessions,
                yearFrom,
                now,
                "year"
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalMinutes", sessions.stream()
                .mapToLong(s -> s.getDurationSeconds() / 60)
                .sum());
        response.put("totalSessions", sessions.size());
        response.put("modules", moduleRows);
        response.put("recentActivity", recent);
        response.put("histograms", Map.of(
                "day", day,
                "month", month,
                "year", year
        ));

        return response;
    }

    private List<Map<String, Object>> bucket(
            List<LearningSession> sessions,
            LocalDateTime from,
            LocalDateTime to,
            String mode
    ) {
        Map<String, Long> values = new LinkedHashMap<>();

        LocalDateTime cursor = from;

        while (!cursor.isAfter(to)) {
            String key;

            if ("day".equals(mode)) {
                key = cursor.toLocalDate().toString();
                cursor = cursor.plusDays(1);
            } else if ("month".equals(mode)) {
                key = YearMonth.from(cursor).toString();
                cursor = cursor.plusMonths(1);
            } else {
                key = String.valueOf(cursor.getYear());
                cursor = cursor.plusYears(1);
            }

            values.putIfAbsent(key, 0L);
        }

        for (LearningSession session : sessions) {
            LocalDateTime started = session.getStartedAt();

            if (started.isBefore(from) || started.isAfter(to)) {
                continue;
            }

            String key;

            if ("day".equals(mode)) {
                key = started.toLocalDate().toString();
            } else if ("month".equals(mode)) {
                key = YearMonth.from(started).toString();
            } else {
                key = String.valueOf(started.getYear());
            }

            if (values.containsKey(key)) {
                values.put(
                        key,
                        values.get(key) + (session.getDurationSeconds() / 60)
                );
            }
        }

        return values.entrySet()
                .stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("label", entry.getKey());
                    row.put("minutes", entry.getValue());
                    return row;
                })
                .toList();
    }

    private LearningSession ownedSession(Long traineeId, Long sessionId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Learning session not found"));

        if (!Objects.equals(session.getTraineeId(), traineeId)) {
            throw new IllegalStateException("You cannot access this learning session");
        }

        return session;
    }

    private void addElapsed(LearningSession session, LocalDateTime now) {
        LocalDateTime last = session.getLastHeartbeatAt() == null
                ? session.getStartedAt()
                : session.getLastHeartbeatAt();

        long seconds = Math.max(0, Duration.between(last, now).getSeconds());

        // Prevent a suspended browser/tab from inflating study time.
        seconds = Math.min(seconds, STALE_SECONDS);

        session.setDurationSeconds(
                Math.max(0L, session.getDurationSeconds()) + seconds
        );
    }

    private void closeStale(Long traineeId) {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(STALE_SECONDS);

        List<LearningSession> active =
                sessionRepository.findByTraineeIdAndActiveTrue(traineeId);

        boolean changed = false;

        for (LearningSession session : active) {
            if (session.getLastHeartbeatAt() == null ||
                    session.getLastHeartbeatAt().isBefore(cutoff)) {

                LocalDateTime effective = session.getLastHeartbeatAt() == null
                        ? session.getStartedAt().plusSeconds(STALE_SECONDS)
                        : session.getLastHeartbeatAt().plusSeconds(STALE_SECONDS);

                addElapsed(session, effective.isAfter(LocalDateTime.now())
                        ? LocalDateTime.now()
                        : effective);

                session.setEndedAt(effective.isAfter(LocalDateTime.now())
                        ? LocalDateTime.now()
                        : effective);

                session.setLastHeartbeatAt(session.getEndedAt());
                session.setActive(false);
                changed = true;
            }
        }

        if (changed) {
            sessionRepository.saveAll(active);
        }
    }
}
