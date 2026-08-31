-- Preserve qualification seed for semi-finals.
ALTER TABLE matches
    ADD COLUMN team_a_seed SMALLINT,
    ADD COLUMN team_b_seed SMALLINT,

    ADD COLUMN source_match_a_id BIGINT,
    ADD COLUMN source_match_b_id BIGINT;


ALTER TABLE matches
    ADD CONSTRAINT chk_team_a_seed
        CHECK (
            team_a_seed IS NULL
            OR team_a_seed > 0
        ),

    ADD CONSTRAINT chk_team_b_seed
        CHECK (
            team_b_seed IS NULL
            OR team_b_seed > 0
        ),

    ADD CONSTRAINT chk_different_source_matches
        CHECK (
            source_match_a_id IS NULL
            OR source_match_b_id IS NULL
            OR source_match_a_id <> source_match_b_id
        );


-- Final participants must come from matches in the same edition.
ALTER TABLE matches
    ADD CONSTRAINT fk_source_match_a
        FOREIGN KEY (
            source_match_a_id,
            tournament_edition_id
        )
        REFERENCES matches(
            id,
            tournament_edition_id
        ),

    ADD CONSTRAINT fk_source_match_b
        FOREIGN KEY (
            source_match_b_id,
            tournament_edition_id
        )
        REFERENCES matches(
            id,
            tournament_edition_id
        );


-- Database-level protection against duplicate finals.
CREATE UNIQUE INDEX uq_one_final_per_edition
    ON matches(tournament_edition_id)
    WHERE stage = 'FINAL';


-- Support manually resolved knockout matches.
ALTER TABLE matches
    DROP CONSTRAINT chk_match_result_type;

ALTER TABLE matches
    ADD CONSTRAINT chk_match_result_type
        CHECK (
            result_type IS NULL
            OR result_type IN (
                'RUNS',
                'WICKETS',
                'TIE',
                'NO_RESULT',
                'TIEBREAKER',
                'FORFEIT'
            )
        );
