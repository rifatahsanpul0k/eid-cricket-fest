-- =========================================================
-- POINTS CONFIGURATION
-- =========================================================

ALTER TABLE tournament_editions
    ADD COLUMN win_points NUMERIC(5, 2) NOT NULL DEFAULT 2,
    ADD COLUMN tie_points NUMERIC(5, 2) NOT NULL DEFAULT 1,
    ADD COLUMN no_result_points NUMERIC(5, 2) NOT NULL DEFAULT 1,
    ADD COLUMN loss_points NUMERIC(5, 2) NOT NULL DEFAULT 0;

ALTER TABLE tournament_editions
    ADD CONSTRAINT chk_edition_points
        CHECK (
            win_points >= 0
            AND tie_points >= 0
            AND no_result_points >= 0
            AND loss_points >= 0
        );


-- We no longer store derived standings values here.
-- Match + innings data is the source of truth.
ALTER TABLE tournament_teams
    DROP COLUMN points,
    DROP COLUMN net_run_rate;
