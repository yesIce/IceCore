ALTER TABLE players
    ADD COLUMN IF NOT EXISTS locale VARCHAR(10);

CREATE INDEX IF NOT EXISTS idx_players_locale ON players (locale) WHERE locale IS NOT NULL;
