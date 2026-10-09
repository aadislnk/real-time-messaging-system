CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    display_name  VARCHAR(80)  NOT NULL,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    enabled_at    TIMESTAMP    NOT NULL,
    updated_at    VARCHAR(100) NOT NULL
)