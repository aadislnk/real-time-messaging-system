ALTER TABLE users
    DROP COLUMN enabled_at,
    ADD COLUMN enabled BOOLEAN NOT NULL,
    ADD COLUMN created_at TIMESTAMP NOT NULL,
    ALTER COLUMN updated_at TYPE TIMESTAMP
                 using updated_at::timestamp;
