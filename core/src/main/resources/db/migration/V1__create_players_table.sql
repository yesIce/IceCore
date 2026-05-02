CREATE TABLE IF NOT EXISTS players (
    uuid             UUID         PRIMARY KEY,
    username         VARCHAR(16)  NOT NULL,
    first_login      TIMESTAMPTZ  NOT NULL,
    last_login       TIMESTAMPTZ  NOT NULL,
    playtime_millis  BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_players_username ON players (LOWER(username));
