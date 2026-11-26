CREATE TABLE skin (
    id         BIGSERIAL   PRIMARY KEY,
    code       VARCHAR(20) NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(20) NOT NULL CHECK (type IN ('AVATAR', 'CARD_VARIANT')),
    card_id    BIGINT REFERENCES card (id),
    rarity_id  SMALLINT NOT NULL REFERENCES rarity (id),
    price      INTEGER NOT NULL CHECK (price >= 0),
    image_file VARCHAR(255) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK ((type = 'AVATAR' AND card_id IS NULL)
        OR (type = 'CARD_VARIANT' AND card_id IS NOT NULL))
);
