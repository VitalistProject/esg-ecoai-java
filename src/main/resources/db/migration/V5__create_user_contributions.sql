CREATE TABLE user_contributions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    points REAL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
