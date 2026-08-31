-- =========================================================
-- PREPARE COMPOSITE REFERENCES
-- Allows us to guarantee that team/player belongs
-- to the same tournament edition.
-- =========================================================

ALTER TABLE tournament_teams
    ADD CONSTRAINT uq_tournament_teams_id_edition
        UNIQUE (id, tournament_edition_id);

ALTER TABLE player_registrations
    ADD CONSTRAINT uq_player_registrations_id_edition
        UNIQUE (id, tournament_edition_id);


-- =========================================================
-- CAPTAIN / VICE CAPTAIN
-- Captain references tournament registration,
-- not the permanent player directly.
-- =========================================================

ALTER TABLE tournament_teams
    ADD COLUMN captain_registration_id BIGINT,
    ADD COLUMN vice_captain_registration_id BIGINT,
    ADD COLUMN roster_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN roster_locked_at TIMESTAMPTZ;


ALTER TABLE tournament_teams
    ADD CONSTRAINT fk_tournament_team_captain
        FOREIGN KEY (
                     captain_registration_id,
                     tournament_edition_id
            )
            REFERENCES player_registrations (
                                             id,
                                             tournament_edition_id
                ),

    ADD CONSTRAINT fk_tournament_team_vice_captain
        FOREIGN KEY (
                     vice_captain_registration_id,
                     tournament_edition_id
            )
            REFERENCES player_registrations (
                                             id,
                                             tournament_edition_id
                ),

    ADD CONSTRAINT chk_roster_status
        CHECK (
            roster_status IN (
                              'OPEN',
                              'LOCKED'
                )
            ),

    ADD CONSTRAINT chk_different_captain_vice_captain
        CHECK (
            captain_registration_id IS NULL
                OR vice_captain_registration_id IS NULL
                OR captain_registration_id <> vice_captain_registration_id
            );


-- =========================================================
-- DRAFT
-- One primary draft per tournament edition.
-- =========================================================

CREATE TABLE drafts (
                        id BIGSERIAL PRIMARY KEY,

                        tournament_edition_id BIGINT NOT NULL,

                        status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                        pick_mode VARCHAR(20) NOT NULL DEFAULT 'LINEAR',

                        started_at TIMESTAMPTZ,
                        completed_at TIMESTAMPTZ,

                        created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_draft_edition
                            FOREIGN KEY (tournament_edition_id)
                                REFERENCES tournament_editions(id),

                        CONSTRAINT uq_draft_edition
                            UNIQUE (tournament_edition_id),

                        CONSTRAINT uq_draft_id_edition
                            UNIQUE (id, tournament_edition_id),

                        CONSTRAINT chk_draft_status
                            CHECK (
                                status IN (
                                           'PENDING',
                                           'ORDER_GENERATED',
                                           'IN_PROGRESS',
                                           'COMPLETED',
                                           'CANCELLED'
                                    )
                                ),

                        CONSTRAINT chk_pick_mode
                            CHECK (
                                pick_mode IN (
                                              'LINEAR',
                                              'SNAKE'
                                    )
                                )
);


-- =========================================================
-- LOTTERY / DRAFT ORDER
-- Example:
-- 1 Tigers
-- 2 Warriors
-- 3 Kings
-- =========================================================

CREATE TABLE draft_orders (
                              id BIGSERIAL PRIMARY KEY,

                              draft_id BIGINT NOT NULL,
                              tournament_edition_id BIGINT NOT NULL,
                              tournament_team_id BIGINT NOT NULL,

                              position INTEGER NOT NULL,

                              created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_draft_order_draft
                                  FOREIGN KEY (
                                               draft_id,
                                               tournament_edition_id
                                      )
                                      REFERENCES drafts (
                                                         id,
                                                         tournament_edition_id
                                          )
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_draft_order_team
                                  FOREIGN KEY (
                                               tournament_team_id,
                                               tournament_edition_id
                                      )
                                      REFERENCES tournament_teams (
                                                                   id,
                                                                   tournament_edition_id
                                          ),

                              CONSTRAINT uq_draft_order_team
                                  UNIQUE (draft_id, tournament_team_id),

                              CONSTRAINT uq_draft_order_position
                                  UNIQUE (draft_id, position),

                              CONSTRAINT chk_draft_position
                                  CHECK (position > 0)
);


-- =========================================================
-- DRAFT PICKS
-- Immutable history of who selected whom.
-- =========================================================

CREATE TABLE draft_picks (
                             id BIGSERIAL PRIMARY KEY,

                             draft_id BIGINT NOT NULL,
                             tournament_edition_id BIGINT NOT NULL,

                             tournament_team_id BIGINT NOT NULL,
                             player_registration_id BIGINT NOT NULL,

                             round_number INTEGER NOT NULL,
                             pick_number INTEGER NOT NULL,

                             selected_by_user_id BIGINT,

                             selected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_draft_pick_draft
                                 FOREIGN KEY (
                                              draft_id,
                                              tournament_edition_id
                                     )
                                     REFERENCES drafts (
                                                        id,
                                                        tournament_edition_id
                                         ),

                             CONSTRAINT fk_draft_pick_team
                                 FOREIGN KEY (
                                              tournament_team_id,
                                              tournament_edition_id
                                     )
                                     REFERENCES tournament_teams (
                                                                  id,
                                                                  tournament_edition_id
                                         ),

                             CONSTRAINT fk_draft_pick_registration
                                 FOREIGN KEY (
                                              player_registration_id,
                                              tournament_edition_id
                                     )
                                     REFERENCES player_registrations (
                                                                      id,
                                                                      tournament_edition_id
                                         ),

                             CONSTRAINT fk_draft_pick_selected_by
                                 FOREIGN KEY (selected_by_user_id)
                                     REFERENCES users(id),

                             CONSTRAINT uq_draft_pick_number
                                 UNIQUE (draft_id, pick_number),

                             CONSTRAINT uq_draft_player
                                 UNIQUE (draft_id, player_registration_id),

                             CONSTRAINT chk_draft_round
                                 CHECK (round_number > 0),

                             CONSTRAINT chk_draft_pick_number
                                 CHECK (pick_number > 0)
);


CREATE INDEX idx_draft_picks_team
    ON draft_picks(tournament_team_id);

CREATE INDEX idx_draft_picks_registration
    ON draft_picks(player_registration_id);


-- =========================================================
-- TEAM ROSTER
-- Current and historical tournament team membership.
-- =========================================================

CREATE TABLE team_roster_entries (
                                     id BIGSERIAL PRIMARY KEY,

                                     tournament_edition_id BIGINT NOT NULL,
                                     tournament_team_id BIGINT NOT NULL,
                                     player_registration_id BIGINT NOT NULL,

                                     acquisition_type VARCHAR(20) NOT NULL,

                                     jersey_number VARCHAR(10),

                                     status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                     joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     removed_at TIMESTAMPTZ,

                                     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_roster_team
                                         FOREIGN KEY (
                                                      tournament_team_id,
                                                      tournament_edition_id
                                             )
                                             REFERENCES tournament_teams (
                                                                          id,
                                                                          tournament_edition_id
                                                 ),

                                     CONSTRAINT fk_roster_registration
                                         FOREIGN KEY (
                                                      player_registration_id,
                                                      tournament_edition_id
                                             )
                                             REFERENCES player_registrations (
                                                                              id,
                                                                              tournament_edition_id
                                                 ),

                                     CONSTRAINT chk_acquisition_type
                                         CHECK (
                                             acquisition_type IN (
                                                                  'CAPTAIN',
                                                                  'DRAFT',
                                                                  'MANUAL'
                                                 )
                                             ),

                                     CONSTRAINT chk_roster_entry_status
                                         CHECK (
                                             status IN (
                                                        'ACTIVE',
                                                        'REMOVED'
                                                 )
                                             ),

                                     CONSTRAINT chk_roster_removed
                                         CHECK (
                                             (status = 'ACTIVE' AND removed_at IS NULL)
                                                 OR
                                             (status = 'REMOVED' AND removed_at IS NOT NULL)
                                             )
);


-- A player can belong to only one ACTIVE team
-- in one tournament edition.
CREATE UNIQUE INDEX uq_active_roster_player
    ON team_roster_entries (
                            tournament_edition_id,
                            player_registration_id
        )
    WHERE status = 'ACTIVE';


CREATE INDEX idx_roster_team
    ON team_roster_entries(tournament_team_id);

CREATE INDEX idx_roster_registration
    ON team_roster_entries(player_registration_id);


-- Optional jersey uniqueness inside an active team.
CREATE UNIQUE INDEX uq_active_team_jersey
    ON team_roster_entries (
                            tournament_team_id,
                            jersey_number
        )
    WHERE status = 'ACTIVE'
        AND jersey_number IS NOT NULL;