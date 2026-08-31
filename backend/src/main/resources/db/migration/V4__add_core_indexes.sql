CREATE UNIQUE INDEX uq_tournaments_name_ci
    ON tournaments (LOWER(name));

CREATE UNIQUE INDEX uq_tournament_editions_name_ci
    ON tournament_editions (tournament_id, LOWER(name));

CREATE INDEX idx_tournament_editions_tournament_id
    ON tournament_editions (tournament_id);