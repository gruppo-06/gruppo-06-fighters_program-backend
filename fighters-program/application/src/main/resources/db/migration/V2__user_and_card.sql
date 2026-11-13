CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    nickname      VARCHAR(20)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL DEFAULT 'PLAYER' CHECK (role IN ('PLAYER', 'ADMIN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE card (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    country     VARCHAR(100),
    faction_id  SMALLINT     NOT NULL REFERENCES faction (id),
    class_id    SMALLINT     NOT NULL REFERENCES fighter_class (id),
    rarity_id   SMALLINT     NOT NULL REFERENCES rarity (id),
    atk         INTEGER      NOT NULL CHECK (atk BETWEEN 0 AND 1000),
    def         INTEGER      NOT NULL CHECK (def BETWEEN 0 AND 1000),
    vel         INTEGER      NOT NULL CHECK (vel BETWEEN 0 AND 1000),
    crt         INTEGER      NOT NULL CHECK (crt BETWEEN 0 AND 1000),
    flavor_text TEXT,
    image_file  VARCHAR(255)
);

CREATE INDEX idx_card_faction ON card (faction_id);
CREATE INDEX idx_card_rarity ON card (rarity_id);
