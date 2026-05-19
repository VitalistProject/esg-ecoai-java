CREATE TABLE goals (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    dimension TEXT NOT NULL CHECK (dimension IN ('environmental', 'social', 'governance')),
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_goals_company_dimension ON goals(company_id, dimension);
