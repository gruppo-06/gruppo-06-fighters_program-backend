CREATE TABLE user_avatar (
    user_id BIGINT PRIMARY KEY REFERENCES app_user (id),
    skin_id BIGINT NOT NULL REFERENCES skin (id)
);

CREATE TABLE user_card_variant (
    user_id BIGINT NOT NULL REFERENCES app_user (id),
    card_id BIGINT NOT NULL REFERENCES card (id),
    skin_id BIGINT NOT NULL REFERENCES skin (id),
    PRIMARY KEY (user_id, card_id)
);
