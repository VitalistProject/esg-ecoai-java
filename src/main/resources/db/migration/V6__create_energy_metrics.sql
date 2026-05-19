CREATE TABLE energy_metrics (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    month TEXT NOT NULL,
    year INTEGER NOT NULL,
    realized_value REAL NOT NULL,
    goal_value REAL NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
