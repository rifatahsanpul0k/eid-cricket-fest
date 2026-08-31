-- =========================================================
-- PREPARE PLAYING XI COMPOSITE REFERENCES
-- =========================================================

ALTER TABLE playing_xi_entries
    ADD CONSTRAINT uq_playing_xi_id_match
        UNIQUE (id, match_id);


-- =========================================================
-- INNINGS
-- =========================================================

CREATE TABLE innings (
    id BIGSERIAL PRIMARY KEY,

    match_id BIGINT NOT NULL,
    tournament_edition_id BIGINT NOT NULL,

    innings_number SMALLINT NOT NULL,

    batting_team_id BIGINT NOT NULL,
    bowling_team_id BIGINT NOT NULL,

    target_runs INTEGER,

    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',

    total_runs INTEGER NOT NULL DEFAULT 0,
    wickets INTEGER NOT NULL DEFAULT 0,
    legal_balls INTEGER NOT NULL DEFAULT 0,

    wide_runs INTEGER NOT NULL DEFAULT 0,
    no_ball_runs INTEGER NOT NULL DEFAULT 0,
    bye_runs INTEGER NOT NULL DEFAULT 0,
    leg_bye_runs INTEGER NOT NULL DEFAULT 0,
    penalty_runs INTEGER NOT NULL DEFAULT 0,

    current_striker_xi_id BIGINT,
    current_non_striker_xi_id BIGINT,
    current_bowler_xi_id BIGINT,

    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_innings_id_match
        UNIQUE (id, match_id),

    CONSTRAINT uq_match_innings
        UNIQUE (match_id, innings_number),

    CONSTRAINT fk_innings_match
        FOREIGN KEY (
            match_id,
            tournament_edition_id
        )
        REFERENCES matches(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_innings_batting_team
        FOREIGN KEY (
            batting_team_id,
            tournament_edition_id
        )
        REFERENCES tournament_teams(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_innings_bowling_team
        FOREIGN KEY (
            bowling_team_id,
            tournament_edition_id
        )
        REFERENCES tournament_teams(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_innings_striker
        FOREIGN KEY (
            current_striker_xi_id,
            match_id
        )
        REFERENCES playing_xi_entries(
            id,
            match_id
        ),

    CONSTRAINT fk_innings_non_striker
        FOREIGN KEY (
            current_non_striker_xi_id,
            match_id
        )
        REFERENCES playing_xi_entries(
            id,
            match_id
        ),

    CONSTRAINT fk_innings_bowler
        FOREIGN KEY (
            current_bowler_xi_id,
            match_id
        )
        REFERENCES playing_xi_entries(
            id,
            match_id
        ),

    CONSTRAINT chk_innings_number
        CHECK (innings_number IN (1, 2)),

    CONSTRAINT chk_innings_different_teams
        CHECK (batting_team_id <> bowling_team_id),

    CONSTRAINT chk_innings_status
        CHECK (
            status IN (
                'IN_PROGRESS',
                'COMPLETED'
            )
        ),

    CONSTRAINT chk_innings_totals
        CHECK (
            total_runs >= 0
            AND wickets >= 0
            AND legal_balls >= 0
            AND wide_runs >= 0
            AND no_ball_runs >= 0
            AND bye_runs >= 0
            AND leg_bye_runs >= 0
            AND penalty_runs >= 0
        ),

    CONSTRAINT chk_different_current_batters
        CHECK (
            current_striker_xi_id IS NULL
            OR current_non_striker_xi_id IS NULL
            OR current_striker_xi_id <> current_non_striker_xi_id
        )
);


CREATE INDEX idx_innings_match
    ON innings(match_id);


-- =========================================================
-- DELIVERIES
-- =========================================================

CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,

    innings_id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,

    sequence_no INTEGER NOT NULL,

    striker_xi_id BIGINT NOT NULL,
    non_striker_xi_id BIGINT NOT NULL,
    bowler_xi_id BIGINT NOT NULL,

    runs_off_bat INTEGER NOT NULL DEFAULT 0,

    wide_runs INTEGER NOT NULL DEFAULT 0,
    no_ball_runs INTEGER NOT NULL DEFAULT 0,
    bye_runs INTEGER NOT NULL DEFAULT 0,
    leg_bye_runs INTEGER NOT NULL DEFAULT 0,
    penalty_runs INTEGER NOT NULL DEFAULT 0,

    total_runs INTEGER GENERATED ALWAYS AS (
        runs_off_bat
        + wide_runs
        + no_ball_runs
        + bye_runs
        + leg_bye_runs
        + penalty_runs
    ) STORED,

    is_legal BOOLEAN GENERATED ALWAYS AS (
        wide_runs = 0
        AND no_ball_runs = 0
    ) STORED,

    -- Strike change caused by this delivery itself.
    -- Over-end strike change is handled separately.
    swap_ends BOOLEAN NOT NULL DEFAULT FALSE,

    created_by_user_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    voided_at TIMESTAMPTZ,
    voided_by_user_id BIGINT,
    void_reason TEXT,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_innings_delivery_sequence
        UNIQUE (innings_id, sequence_no),

    CONSTRAINT fk_delivery_innings
        FOREIGN KEY (
            innings_id,
            match_id
        )
        REFERENCES innings(
            id,
            match_id
        ),

    CONSTRAINT fk_delivery_striker
        FOREIGN KEY (
            striker_xi_id,
            match_id
        )
        REFERENCES playing_xi_entries(
            id,
            match_id
        ),

    CONSTRAINT fk_delivery_non_striker
        FOREIGN KEY (
            non_striker_xi_id,
            match_id
        )
        REFERENCES playing_xi_entries(
            id,
            match_id
        ),

    CONSTRAINT fk_delivery_bowler
        FOREIGN KEY (
            bowler_xi_id,
            match_id
        )
        REFERENCES playing_xi_entries(
            id,
            match_id
        ),

    CONSTRAINT fk_delivery_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_delivery_voided_by
        FOREIGN KEY (voided_by_user_id)
        REFERENCES users(id),

    CONSTRAINT chk_delivery_runs
        CHECK (
            runs_off_bat >= 0
            AND wide_runs >= 0
            AND no_ball_runs >= 0
            AND bye_runs >= 0
            AND leg_bye_runs >= 0
            AND penalty_runs >= 0
        ),

    -- No-ball overrides Wide.
    CONSTRAINT chk_not_wide_and_no_ball
        CHECK (
            NOT (
                wide_runs > 0
                AND no_ball_runs > 0
            )
        ),

    CONSTRAINT chk_not_bye_and_leg_bye
        CHECK (
            NOT (
                bye_runs > 0
                AND leg_bye_runs > 0
            )
        ),

    CONSTRAINT chk_bye_not_bat_runs
        CHECK (
            (bye_runs = 0 AND leg_bye_runs = 0)
            OR runs_off_bat = 0
        ),

    -- All runs resulting from a Wide are scored as wides.
    CONSTRAINT chk_wide_scoring
        CHECK (
            wide_runs = 0
            OR (
                runs_off_bat = 0
                AND bye_runs = 0
                AND leg_bye_runs = 0
            )
        )
);


CREATE INDEX idx_deliveries_innings
    ON deliveries(innings_id, sequence_no);

CREATE INDEX idx_active_deliveries
    ON deliveries(innings_id)
    WHERE voided_at IS NULL;


-- =========================================================
-- WICKETS
-- =========================================================

CREATE TABLE wickets (
    id BIGSERIAL PRIMARY KEY,

    delivery_id BIGINT NOT NULL UNIQUE,
    match_id BIGINT NOT NULL,

    dismissed_playing_xi_id BIGINT NOT NULL,

    dismissal_type VARCHAR(40) NOT NULL,

    fielder_playing_xi_id BIGINT,

    credited_to_bowler BOOLEAN NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wicket_delivery
        FOREIGN KEY (delivery_id)
        REFERENCES deliveries(id),

    CONSTRAINT fk_wicket_dismissed_player
        FOREIGN KEY (
            dismissed_playing_xi_id,
            match_id
        )
        REFERENCES playing_xi_entries(
            id,
            match_id
        ),

    CONSTRAINT fk_wicket_fielder
        FOREIGN KEY (
            fielder_playing_xi_id,
            match_id
        )
        REFERENCES playing_xi_entries(
            id,
            match_id
        ),

    CONSTRAINT chk_dismissal_type
        CHECK (
            dismissal_type IN (
                'BOWLED',
                'CAUGHT',
                'LBW',
                'RUN_OUT',
                'STUMPED',
                'HIT_WICKET',
                'OBSTRUCTING_FIELD'
            )
        )
);
