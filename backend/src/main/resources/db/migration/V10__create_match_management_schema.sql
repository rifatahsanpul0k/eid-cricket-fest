-- =========================================================
-- TOURNAMENT MATCH CONFIG
-- =========================================================

ALTER TABLE tournament_editions
    ADD COLUMN playing_xi_size INTEGER NOT NULL DEFAULT 11;

ALTER TABLE tournament_editions
    ADD CONSTRAINT chk_playing_xi_size
        CHECK (
            playing_xi_size >= 2
            AND playing_xi_size <= squad_size
        );


-- =========================================================
-- VENUES
-- =========================================================

CREATE TABLE venues (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    address TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_venues_name_ci
    ON venues (LOWER(name));


-- =========================================================
-- MATCHES
-- =========================================================

CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,

    tournament_edition_id BIGINT NOT NULL,

    team_a_id BIGINT NOT NULL,
    team_b_id BIGINT NOT NULL,

    venue_id BIGINT,

    stage VARCHAR(30) NOT NULL,

    round_number INTEGER,
    match_number INTEGER NOT NULL,

    scheduled_at TIMESTAMPTZ,

    actual_started_at TIMESTAMPTZ,
    actual_ended_at TIMESTAMPTZ,

    overs_per_innings INTEGER NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',

    winner_team_id BIGINT,

    result_type VARCHAR(30),
    winning_margin INTEGER,
    result_summary TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_match_id_edition
        UNIQUE (id, tournament_edition_id),

    CONSTRAINT uq_match_number
        UNIQUE (tournament_edition_id, match_number),

    CONSTRAINT fk_match_edition
        FOREIGN KEY (tournament_edition_id)
        REFERENCES tournament_editions(id),

    CONSTRAINT fk_match_team_a
        FOREIGN KEY (
            team_a_id,
            tournament_edition_id
        )
        REFERENCES tournament_teams(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_match_team_b
        FOREIGN KEY (
            team_b_id,
            tournament_edition_id
        )
        REFERENCES tournament_teams(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_match_winner
        FOREIGN KEY (
            winner_team_id,
            tournament_edition_id
        )
        REFERENCES tournament_teams(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_match_venue
        FOREIGN KEY (venue_id)
        REFERENCES venues(id),

    CONSTRAINT chk_match_different_teams
        CHECK (team_a_id <> team_b_id),

    CONSTRAINT chk_match_number
        CHECK (match_number > 0),

    CONSTRAINT chk_match_round
        CHECK (
            round_number IS NULL
            OR round_number > 0
        ),

    CONSTRAINT chk_match_overs
        CHECK (overs_per_innings > 0),

    CONSTRAINT chk_match_stage
        CHECK (
            stage IN (
                'LEAGUE',
                'SEMI_FINAL',
                'FINAL',
                'OTHER'
            )
        ),

    CONSTRAINT chk_match_status
        CHECK (
            status IN (
                'PLANNED',
                'SCHEDULED',
                'READY',
                'TOSS_COMPLETED',
                'LIVE',
                'INNINGS_BREAK',
                'COMPLETED',
                'POSTPONED',
                'ABANDONED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_match_result_type
        CHECK (
            result_type IS NULL
            OR result_type IN (
                'RUNS',
                'WICKETS',
                'TIE',
                'NO_RESULT'
            )
        ),

    CONSTRAINT chk_winning_margin
        CHECK (
            winning_margin IS NULL
            OR winning_margin >= 0
        )
);


CREATE INDEX idx_matches_edition
    ON matches(tournament_edition_id);

CREATE INDEX idx_matches_status
    ON matches(status);

CREATE INDEX idx_matches_scheduled
    ON matches(scheduled_at);


-- =========================================================
-- MATCH SCORERS
-- =========================================================

CREATE TABLE match_scorers (
    match_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    primary_scorer BOOLEAN NOT NULL DEFAULT FALSE,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by_user_id BIGINT,

    PRIMARY KEY (match_id, user_id),

    CONSTRAINT fk_match_scorer_match
        FOREIGN KEY (match_id)
        REFERENCES matches(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_match_scorer_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_match_scorer_assigned_by
        FOREIGN KEY (assigned_by_user_id)
        REFERENCES users(id)
);


CREATE UNIQUE INDEX uq_primary_match_scorer
    ON match_scorers(match_id)
    WHERE primary_scorer = TRUE;


-- =========================================================
-- TOSS
-- =========================================================

CREATE TABLE match_tosses (
    id BIGSERIAL PRIMARY KEY,

    match_id BIGINT NOT NULL UNIQUE,
    tournament_edition_id BIGINT NOT NULL,

    winner_team_id BIGINT NOT NULL,

    decision VARCHAR(10) NOT NULL,

    recorded_by_user_id BIGINT,

    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_toss_match
        FOREIGN KEY (
            match_id,
            tournament_edition_id
        )
        REFERENCES matches(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_toss_winner
        FOREIGN KEY (
            winner_team_id,
            tournament_edition_id
        )
        REFERENCES tournament_teams(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_toss_recorded_by
        FOREIGN KEY (recorded_by_user_id)
        REFERENCES users(id),

    CONSTRAINT chk_toss_decision
        CHECK (decision IN ('BAT', 'BOWL'))
);


-- =========================================================
-- PLAYING XI
-- =========================================================

CREATE TABLE playing_xi_entries (
    id BIGSERIAL PRIMARY KEY,

    match_id BIGINT NOT NULL,
    tournament_edition_id BIGINT NOT NULL,

    tournament_team_id BIGINT NOT NULL,
    player_registration_id BIGINT NOT NULL,

    is_captain BOOLEAN NOT NULL DEFAULT FALSE,
    is_wicketkeeper BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_playing_xi_match
        FOREIGN KEY (
            match_id,
            tournament_edition_id
        )
        REFERENCES matches(
            id,
            tournament_edition_id
        )
        ON DELETE CASCADE,

    CONSTRAINT fk_playing_xi_team
        FOREIGN KEY (
            tournament_team_id,
            tournament_edition_id
        )
        REFERENCES tournament_teams(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_playing_xi_registration
        FOREIGN KEY (
            player_registration_id,
            tournament_edition_id
        )
        REFERENCES player_registrations(
            id,
            tournament_edition_id
        ),

    CONSTRAINT uq_match_player
        UNIQUE (
            match_id,
            player_registration_id
        )
);


CREATE UNIQUE INDEX uq_match_team_captain
    ON playing_xi_entries(
        match_id,
        tournament_team_id
    )
    WHERE is_captain = TRUE;


CREATE UNIQUE INDEX uq_match_team_wicketkeeper
    ON playing_xi_entries(
        match_id,
        tournament_team_id
    )
    WHERE is_wicketkeeper = TRUE;
