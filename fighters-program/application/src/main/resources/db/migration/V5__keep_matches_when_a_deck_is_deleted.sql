-- A finished match keeps a snapshot of both decks, so deleting a deck must not delete or block its matches.
ALTER TABLE match ALTER COLUMN challenger_deck_id DROP NOT NULL;

ALTER TABLE match DROP CONSTRAINT match_challenger_deck_id_fkey;
ALTER TABLE match ADD CONSTRAINT match_challenger_deck_id_fkey
    FOREIGN KEY (challenger_deck_id) REFERENCES deck (id) ON DELETE SET NULL;

ALTER TABLE match DROP CONSTRAINT match_opponent_deck_id_fkey;
ALTER TABLE match ADD CONSTRAINT match_opponent_deck_id_fkey
    FOREIGN KEY (opponent_deck_id) REFERENCES deck (id) ON DELETE SET NULL;

ALTER TABLE match ADD COLUMN challenger_deck_snapshot JSONB;
ALTER TABLE match ADD COLUMN opponent_deck_snapshot JSONB;
