CREATE TABLE tournament_player_awards (
    id BIGSERIAL PRIMARY KEY,

    tournament_edition_id BIGINT NOT NULL,
    player_registration_id BIGINT NOT NULL,

    award_type VARCHAR(40) NOT NULL,

    -- Mainly for CUSTOM awards.
    title VARCHAR(150),

    notes TEXT,

    awarded_by_user_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_award_edition
        FOREIGN KEY (tournament_edition_id)
        REFERENCES tournament_editions(id),

    CONSTRAINT fk_award_registration
        FOREIGN KEY (
            player_registration_id,
            tournament_edition_id
        )
        REFERENCES player_registrations(
            id,
            tournament_edition_id
        ),

    CONSTRAINT fk_award_awarded_by
        FOREIGN KEY (awarded_by_user_id)
        REFERENCES users(id),

    CONSTRAINT chk_award_type
        CHECK (
            award_type IN (
                'PLAYER_OF_TOURNAMENT',
                'FINAL_MVP',
                'BEST_FIELDER',
                'EMERGING_PLAYER',
                'CUSTOM'
            )
        ),

    CONSTRAINT chk_custom_award_title
        CHECK (
            award_type <> 'CUSTOM'
            OR (
                title IS NOT NULL
                AND LENGTH(TRIM(title)) > 0
            )
        ),

    CONSTRAINT uq_player_award
        UNIQUE (
            tournament_edition_id,
            award_type,
            player_registration_id
        )
);

CREATE INDEX idx_awards_edition
    ON tournament_player_awards(tournament_edition_id);

CREATE INDEX idx_awards_player
    ON tournament_player_awards(player_registration_id);
