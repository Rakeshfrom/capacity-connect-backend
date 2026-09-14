ALTER TABLE trainer_applications
    ADD COLUMN IF NOT EXISTS assessment_json TEXT;

ALTER TABLE trainer_applications
    ADD COLUMN IF NOT EXISTS assessment_score INTEGER;

ALTER TABLE trainer_applications
    ADD COLUMN IF NOT EXISTS assessment_passed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE trainer_applications
    ADD COLUMN IF NOT EXISTS assessment_assigned_at TIMESTAMP;

ALTER TABLE trainer_applications
    ADD COLUMN IF NOT EXISTS assessment_completed_at TIMESTAMP;
