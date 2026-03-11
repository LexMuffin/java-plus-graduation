CREATE TABLE IF NOT EXISTS user_event_interactions (
id BIGSERIAL PRIMARY KEY,
user_id BIGINT NOT NULL,
event_id BIGINT NOT NULL,
weight DOUBLE PRECISION,
timestamp TIMESTAMP,
UNIQUE(user_id, event_id)
);

CREATE TABLE IF NOT EXISTS event_similarities (
id BIGSERIAL PRIMARY KEY,
event_a BIGINT NOT NULL,
event_b BIGINT NOT NULL,
score DOUBLE PRECISION,
UNIQUE(event_a, event_b)
);