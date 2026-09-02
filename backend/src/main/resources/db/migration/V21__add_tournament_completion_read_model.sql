ALTER TABLE tournament_editions
    ADD COLUMN champion_tournament_team_id BIGINT,
    ADD COLUMN runner_up_tournament_team_id BIGINT,
    ADD COLUMN final_match_id BIGINT,
    ADD COLUMN completed_at TIMESTAMPTZ;

ALTER TABLE tournament_editions
    ADD CONSTRAINT fk_tournament_editions_champion_team
        FOREIGN KEY (champion_tournament_team_id)
        REFERENCES tournament_teams(id),
    ADD CONSTRAINT fk_tournament_editions_runner_up_team
        FOREIGN KEY (runner_up_tournament_team_id)
        REFERENCES tournament_teams(id),
    ADD CONSTRAINT fk_tournament_editions_final_match
        FOREIGN KEY (final_match_id)
        REFERENCES matches(id);

CREATE INDEX idx_tournament_editions_champion_team
    ON tournament_editions(champion_tournament_team_id);

CREATE INDEX idx_tournament_editions_final_match
    ON tournament_editions(final_match_id);
