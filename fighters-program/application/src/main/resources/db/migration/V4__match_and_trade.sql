CREATE TABLE match (
    id                 BIGSERIAL   PRIMARY KEY,
    challenger_id      BIGINT      NOT NULL REFERENCES app_user (id),
    opponent_id        BIGINT      NOT NULL REFERENCES app_user (id),
    challenger_deck_id BIGINT      NOT NULL REFERENCES deck (id),
    opponent_deck_id   BIGINT      REFERENCES deck (id),
    status             VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'FINISHED', 'DECLINED', 'CANCELLED')),
    winner_id          BIGINT      REFERENCES app_user (id),
    seed               BIGINT,
    log                JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at        TIMESTAMPTZ,
    CHECK (challenger_id <> opponent_id),
    CHECK (winner_id IS NULL OR winner_id IN (challenger_id, opponent_id)),
    CHECK (status <> 'FINISHED' OR finished_at IS NOT NULL)
);

CREATE INDEX idx_match_challenger ON match (challenger_id);
CREATE INDEX idx_match_opponent ON match (opponent_id);

CREATE TABLE trade (
    id           BIGSERIAL   PRIMARY KEY,
    sender_id    BIGINT      NOT NULL REFERENCES app_user (id),
    recipient_id BIGINT      NOT NULL REFERENCES app_user (id),
    status       VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at  TIMESTAMPTZ,
    CHECK (sender_id <> recipient_id)
);

CREATE INDEX idx_trade_sender ON trade (sender_id);
CREATE INDEX idx_trade_recipient ON trade (recipient_id);

CREATE TABLE trade_item (
    trade_id BIGINT      NOT NULL REFERENCES trade (id) ON DELETE CASCADE,
    card_id  BIGINT      NOT NULL REFERENCES card (id),
    quantity INTEGER     NOT NULL CHECK (quantity >= 1),
    side     VARCHAR(10) NOT NULL CHECK (side IN ('OFFERED', 'REQUESTED')),
    PRIMARY KEY (trade_id, card_id, side)
);
