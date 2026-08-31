ALTER TABLE matches
    ALTER COLUMN team_a_seed TYPE INTEGER
        USING team_a_seed::INTEGER,

    ALTER COLUMN team_b_seed TYPE INTEGER
        USING team_b_seed::INTEGER;