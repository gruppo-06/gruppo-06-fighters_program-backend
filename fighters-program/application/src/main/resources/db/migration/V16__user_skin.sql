CREATE TABLE user_skin (
    user_id     BIGINT      NOT NULL REFERENCES app_user (id),
    skin_id     BIGINT      NOT NULL REFERENCES skin (id),
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, skin_id)
);
