ALTER TABLE tournament_editions
    ADD COLUMN squad_size INTEGER NOT NULL DEFAULT 11;

ALTER TABLE tournament_editions
    ADD CONSTRAINT chk_squad_size
        CHECK (squad_size >= 2);
