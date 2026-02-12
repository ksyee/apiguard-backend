CREATE TABLE check_results (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES endpoints(id),
    status VARCHAR(20) NOT NULL,
    status_code INTEGER,
    response_time_ms BIGINT,
    error_message TEXT,
    checked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_check_results_endpoint_checked ON check_results(endpoint_id, checked_at DESC);
