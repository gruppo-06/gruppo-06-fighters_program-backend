CREATE TABLE user_card (
    user_id  BIGINT  NOT NULL REFERENCES app_user (id),
    card_id  BIGINT  NOT NULL REFERENCES card (id),
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    PRIMARY KEY (user_id, card_id)
);

CREATE INDEX idx_user_card_card ON user_card (card_id);

CREATE TABLE deck (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id),
    name       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);

CREATE TABLE deck_card (
    deck_id BIGINT  NOT NULL REFERENCES deck (id) ON DELETE CASCADE,
    card_id BIGINT  NOT NULL REFERENCES card (id),
    copies  INTEGER NOT NULL CHECK (copies >= 1),
    PRIMARY KEY (deck_id, card_id)
);

CREATE INDEX idx_deck_card_card ON deck_card (card_id);
