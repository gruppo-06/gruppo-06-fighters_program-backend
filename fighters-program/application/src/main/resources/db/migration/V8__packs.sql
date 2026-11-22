CREATE TABLE pack (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    price       INTEGER      NOT NULL CHECK (price >= 0),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE pack_content (
    pack_id     BIGINT      NOT NULL REFERENCES pack (id) ON DELETE CASCADE,
    rarity_id   SMALLINT    NOT NULL REFERENCES rarity (id),
    card_count  INTEGER     NOT NULL CHECK (card_count >= 1),
    PRIMARY KEY (pack_id, rarity_id)
);

CREATE TABLE pack_opening (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES app_user (id),
    pack_id             BIGINT       NOT NULL REFERENCES pack (id),
    cards               JSONB        NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_pack_opening_user ON pack_opening (user_id);