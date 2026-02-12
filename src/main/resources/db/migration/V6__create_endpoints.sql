CREATE TABLE endpoints (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id),
    url VARCHAR(2048) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    headers JSONB,
    body TEXT,
    expected_status_code INTEGER NOT NULL DEFAULT 200,
    check_interval INTEGER NOT NULL DEFAULT 60,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_checked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_endpoints_project_id ON endpoints(project_id);
