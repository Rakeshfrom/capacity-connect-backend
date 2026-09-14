package com.capacityconnect.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class TrainerSchemaRepairConfig {

    @Bean
    ApplicationRunner repairTrainerApplicationSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("""
                ALTER TABLE trainer_applications
                ADD COLUMN IF NOT EXISTS assessment_json TEXT
            """);

            jdbcTemplate.execute("""
                ALTER TABLE trainer_applications
                ADD COLUMN IF NOT EXISTS assessment_score INTEGER
            """);

            jdbcTemplate.execute("""
                ALTER TABLE trainer_applications
                ADD COLUMN IF NOT EXISTS assessment_passed BOOLEAN NOT NULL DEFAULT FALSE
            """);

            jdbcTemplate.execute("""
                ALTER TABLE trainer_applications
                ADD COLUMN IF NOT EXISTS assessment_assigned_at TIMESTAMP
            """);

            jdbcTemplate.execute("""
                ALTER TABLE trainer_applications
                ADD COLUMN IF NOT EXISTS assessment_completed_at TIMESTAMP
            """);

            System.out.println("Trainer application schema verification completed.");
        };
    }
}
