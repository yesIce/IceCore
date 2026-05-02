CREATE TABLE friendships (
    first_uuid      UUID            NOT NULL,
    second_uuid     UUID            NOT NULL,
    since           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    PRIMARY KEY (first_uuid, second_uuid),
    CHECK (first_uuid < second_uuid)
);

CREATE INDEX idx_friendships_first_uuid ON friendships (first_uuid);
CREATE INDEX idx_friendships_second_uuid ON friendships (second_uuid);