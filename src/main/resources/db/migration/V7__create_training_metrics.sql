CREATE TABLE training_metrics (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    quarter TEXT NOT NULL,
    year INTEGER NOT NULL,
    total_hours REAL NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
