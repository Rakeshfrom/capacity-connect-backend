package com.capacityconnect.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LearningSessionSchemaRepairConfig {

    private final JdbcTemplate jdbcTemplate;

    public LearningSessionSchemaRepairConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS learning_sessions (
                id BIGSERIAL PRIMARY KEY,
                trainee_id BIGINT NOT NULL,
                course_id BIGINT NOT NULL,
                module_id BIGINT NOT NULL,
                started_at TIMESTAMP NOT NULL,
                last_heartbeat_at TIMESTAMP NOT NULL,
                ended_at TIMESTAMP NULL,
                duration_seconds BIGINT NOT NULL DEFAULT 0,
                active BOOLEAN NOT NULL DEFAULT TRUE
            )
        """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_learning_session_trainee
            ON learning_sessions (trainee_id)
        """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_learning_session_module
            ON learning_sessions (module_id)
        """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_learning_session_started
            ON learning_sessions (started_at)
        """);
    }
}
