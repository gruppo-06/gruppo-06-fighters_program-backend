CREATE TABLE friendship (
    id           BIGSERIAL   PRIMARY KEY,
    requester_id BIGINT      NOT NULL REFERENCES app_user (id),
    addressee_id BIGINT      NOT NULL REFERENCES app_user (id),
    status       VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at  TIMESTAMPTZ,
    CHECK (requester_id <> addressee_id)
);

CREATE UNIQUE INDEX friendship_pair_key
    ON friendship (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
