CREATE TABLE tournaments (
                             id BIGSERIAL PRIMARY KEY,

                             name VARCHAR(150) NOT NULL,
                             description TEXT,
                             logo_url TEXT,

                             created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE tournament_editions (
                                     id BIGSERIAL PRIMARY KEY,

                                     tournament_id BIGINT NOT NULL,

                                     name VARCHAR(150) NOT NULL,

                                     start_date DATE,
                                     end_date DATE,

                                     registration_start_at TIMESTAMPTZ,
                                     registration_end_at TIMESTAMPTZ,

                                     overs_per_innings INTEGER NOT NULL,

                                     status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

                                     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_tournament_editions_tournament
                                         FOREIGN KEY (tournament_id)
                                             REFERENCES tournaments(id),

                                     CONSTRAINT chk_tournament_dates
                                         CHECK (
                                             start_date IS NULL
                                                 OR end_date IS NULL
                                                 OR end_date >= start_date
                                             ),

                                     CONSTRAINT chk_overs_per_innings
                                         CHECK (overs_per_innings > 0)
);


CREATE TABLE teams (
                       id BIGSERIAL PRIMARY KEY,

                       name VARCHAR(120) NOT NULL,
                       short_name VARCHAR(20),
                       logo_url TEXT,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT uq_team_name UNIQUE (name)
);


CREATE TABLE tournament_teams (
                                  id BIGSERIAL PRIMARY KEY,

                                  tournament_edition_id BIGINT NOT NULL,
                                  team_id BIGINT NOT NULL,

                                  points NUMERIC(8, 2) NOT NULL DEFAULT 0,
                                  net_run_rate NUMERIC(10, 4) NOT NULL DEFAULT 0,

                                  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_tournament_team_edition
                                      FOREIGN KEY (tournament_edition_id)
                                          REFERENCES tournament_editions(id),

                                  CONSTRAINT fk_tournament_team_team
                                      FOREIGN KEY (team_id)
                                          REFERENCES teams(id),

                                  CONSTRAINT uq_tournament_team
                                      UNIQUE (tournament_edition_id, team_id)
);