CREATE TABLE credit_transaction (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES app_user (id),
    amount          INTEGER      NOT NULL CHECK (amount <> 0),
    type            VARCHAR(20)  NOT NULL CHECK (type IN ('WELCOME', 'DAILY', 'MATCH_WIN', 'PACK_PURCHASE',
                                                          'ADMIN_GRANT', 'SKIN_PURCHASE', 'TROPHY',
                                                          'DAILY_CHALLENGE', 'BATTLE_PASS')),
    reference       VARCHAR(255),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_credit_transaction_user ON credit_transaction (user_id);
