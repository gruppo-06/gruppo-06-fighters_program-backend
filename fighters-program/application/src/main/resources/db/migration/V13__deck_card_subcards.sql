CREATE TABLE deck_card_subcard (
    deck_id     BIGINT   NOT NULL,
    card_id     BIGINT   NOT NULL,
    subcard_id  SMALLINT NOT NULL REFERENCES subcard (id),
    PRIMARY KEY (deck_id, card_id, subcard_id),
    FOREIGN KEY (deck_id, card_id) REFERENCES deck_card (deck_id, card_id) ON DELETE CASCADE
);
