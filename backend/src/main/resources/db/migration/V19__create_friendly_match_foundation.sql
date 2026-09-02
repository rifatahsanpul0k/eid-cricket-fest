ALTER TABLE matches
    ADD COLUMN match_type VARCHAR(30) NOT NULL DEFAULT 'TOURNAMENT';

ALTER TABLE matches
    ALTER COLUMN tournament_edition_id DROP NOT NULL,
    ALTER COLUMN team_a_id DROP NOT NULL,
    ALTER COLUMN team_b_id DROP NOT NULL,
    ALTER COLUMN stage DROP NOT NULL,
    ALTER COLUMN match_number DROP NOT NULL;

CREATE TABLE match_sides (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL,
    side_key CHAR(1) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    tournament_team_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_match_side_match
        FOREIGN KEY (match_id)
        REFERENCES matches(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_match_side_tournament_team
        FOREIGN KEY (tournament_team_id)
        REFERENCES tournament_teams(id),

    CONSTRAINT uq_match_side_key
        UNIQUE (match_id, side_key),

    CONSTRAINT uq_match_side_id_match
        UNIQUE (id, match_id),

    CONSTRAINT chk_match_side_key
        CHECK (side_key IN ('A', 'B')),

    CONSTRAINT chk_match_side_display_name
        CHECK (length(trim(display_name)) > 0)
);

INSERT INTO match_sides (
    match_id,
    side_key,
    display_name,
    tournament_team_id
)
SELECT m.id,
       'A',
       t.name,
       m.team_a_id
FROM matches m
JOIN tournament_teams tt ON tt.id = m.team_a_id
JOIN teams t ON t.id = tt.team_id;

INSERT INTO match_sides (
    match_id,
    side_key,
    display_name,
    tournament_team_id
)
SELECT m.id,
       'B',
       t.name,
       m.team_b_id
FROM matches m
JOIN tournament_teams tt ON tt.id = m.team_b_id
JOIN teams t ON t.id = tt.team_id;

ALTER TABLE matches
    ADD COLUMN team_a_side_id BIGINT,
    ADD COLUMN team_b_side_id BIGINT,
    ADD COLUMN winner_side_id BIGINT;

UPDATE matches m
SET team_a_side_id = s.id
FROM match_sides s
WHERE s.match_id = m.id
  AND s.side_key = 'A';

UPDATE matches m
SET team_b_side_id = s.id
FROM match_sides s
WHERE s.match_id = m.id
  AND s.side_key = 'B';

UPDATE matches m
SET winner_side_id = s.id
FROM match_sides s
WHERE s.match_id = m.id
  AND s.tournament_team_id = m.winner_team_id;

ALTER TABLE matches
    ADD CONSTRAINT fk_match_team_a_side
        FOREIGN KEY (team_a_side_id, id)
        REFERENCES match_sides(id, match_id),
    ADD CONSTRAINT fk_match_team_b_side
        FOREIGN KEY (team_b_side_id, id)
        REFERENCES match_sides(id, match_id),
    ADD CONSTRAINT fk_match_winner_side
        FOREIGN KEY (winner_side_id, id)
        REFERENCES match_sides(id, match_id),
    ADD CONSTRAINT chk_match_type
        CHECK (match_type IN ('TOURNAMENT', 'FRIENDLY')),
    ADD CONSTRAINT chk_match_type_participants
        CHECK (
            (
                match_type = 'TOURNAMENT'
                AND tournament_edition_id IS NOT NULL
                AND team_a_id IS NOT NULL
                AND team_b_id IS NOT NULL
                AND stage IS NOT NULL
                AND match_number IS NOT NULL
            )
            OR
            (
                match_type = 'FRIENDLY'
                AND tournament_edition_id IS NULL
                AND team_a_id IS NULL
                AND team_b_id IS NULL
                AND stage IS NULL
                AND match_number IS NULL
            )
        );

ALTER TABLE playing_xi_entries
    ADD COLUMN match_side_id BIGINT,
    ADD COLUMN player_id BIGINT;

UPDATE playing_xi_entries xi
SET player_id = pr.player_id
FROM player_registrations pr
WHERE pr.id = xi.player_registration_id;

UPDATE playing_xi_entries xi
SET match_side_id = s.id
FROM match_sides s
WHERE s.match_id = xi.match_id
  AND s.tournament_team_id = xi.tournament_team_id;

ALTER TABLE playing_xi_entries
    ALTER COLUMN match_side_id SET NOT NULL,
    ALTER COLUMN player_id SET NOT NULL,
    ALTER COLUMN tournament_edition_id DROP NOT NULL,
    ALTER COLUMN tournament_team_id DROP NOT NULL,
    ALTER COLUMN player_registration_id DROP NOT NULL,
    ADD CONSTRAINT fk_playing_xi_match_only
        FOREIGN KEY (match_id)
        REFERENCES matches(id)
        ON DELETE CASCADE,
    ADD CONSTRAINT fk_playing_xi_side
        FOREIGN KEY (match_side_id, match_id)
        REFERENCES match_sides(id, match_id),
    ADD CONSTRAINT fk_playing_xi_player
        FOREIGN KEY (player_id)
        REFERENCES players(id);

ALTER TABLE playing_xi_entries
    DROP CONSTRAINT uq_match_player;

ALTER TABLE playing_xi_entries
    ADD CONSTRAINT uq_match_player
        UNIQUE (match_id, player_id);

DROP INDEX uq_match_team_captain;
DROP INDEX uq_match_team_wicketkeeper;

CREATE UNIQUE INDEX uq_match_side_captain
    ON playing_xi_entries(match_id, match_side_id)
    WHERE is_captain = TRUE;

CREATE UNIQUE INDEX uq_match_side_wicketkeeper
    ON playing_xi_entries(match_id, match_side_id)
    WHERE is_wicketkeeper = TRUE;

ALTER TABLE match_tosses
    ADD COLUMN winner_match_side_id BIGINT;

UPDATE match_tosses mt
SET winner_match_side_id = s.id
FROM match_sides s
WHERE s.match_id = mt.match_id
  AND s.tournament_team_id = mt.winner_team_id;

ALTER TABLE match_tosses
    ALTER COLUMN winner_match_side_id SET NOT NULL,
    ALTER COLUMN tournament_edition_id DROP NOT NULL,
    ALTER COLUMN winner_team_id DROP NOT NULL,
    ADD CONSTRAINT fk_toss_match_only
        FOREIGN KEY (match_id)
        REFERENCES matches(id)
        ON DELETE CASCADE,
    ADD CONSTRAINT fk_toss_winner_side
        FOREIGN KEY (winner_match_side_id, match_id)
        REFERENCES match_sides(id, match_id);

ALTER TABLE innings
    ADD COLUMN batting_match_side_id BIGINT,
    ADD COLUMN bowling_match_side_id BIGINT;

UPDATE innings i
SET batting_match_side_id = s.id
FROM match_sides s
WHERE s.match_id = i.match_id
  AND s.tournament_team_id = i.batting_team_id;

UPDATE innings i
SET bowling_match_side_id = s.id
FROM match_sides s
WHERE s.match_id = i.match_id
  AND s.tournament_team_id = i.bowling_team_id;

ALTER TABLE innings
    ALTER COLUMN batting_match_side_id SET NOT NULL,
    ALTER COLUMN bowling_match_side_id SET NOT NULL,
    ALTER COLUMN tournament_edition_id DROP NOT NULL,
    ALTER COLUMN batting_team_id DROP NOT NULL,
    ALTER COLUMN bowling_team_id DROP NOT NULL,
    ADD CONSTRAINT fk_innings_match_only
        FOREIGN KEY (match_id)
        REFERENCES matches(id)
        ON DELETE CASCADE,
    ADD CONSTRAINT fk_innings_batting_side
        FOREIGN KEY (batting_match_side_id, match_id)
        REFERENCES match_sides(id, match_id),
    ADD CONSTRAINT fk_innings_bowling_side
        FOREIGN KEY (bowling_match_side_id, match_id)
        REFERENCES match_sides(id, match_id),
    ADD CONSTRAINT chk_innings_different_sides
        CHECK (batting_match_side_id <> bowling_match_side_id);

CREATE INDEX idx_match_sides_match
    ON match_sides(match_id);

CREATE INDEX idx_playing_xi_match_side
    ON playing_xi_entries(match_id, match_side_id);

CREATE INDEX idx_playing_xi_player
    ON playing_xi_entries(player_id);
