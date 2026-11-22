CREATE TABLE daily_claim (
    user_id         BIGINT  PRIMARY KEY REFERENCES app_user (id),
    last_claim_date DATE    NOT NULL,
    streak          INTEGER NOT NULL CHECK (streak >= 1)
);
