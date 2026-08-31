-- =========================================================
-- USERS
-- =========================================================

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       display_name VARCHAR(120) NOT NULL,
                       email VARCHAR(255),
                       phone VARCHAR(30),

                       password_hash VARCHAR(255) NOT NULL,

                       enabled BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT chk_user_contact
                           CHECK (email IS NOT NULL OR phone IS NOT NULL)
);


-- Case-insensitive email uniqueness
CREATE UNIQUE INDEX uq_users_email_ci
    ON users (LOWER(email))
    WHERE email IS NOT NULL;


CREATE UNIQUE INDEX uq_users_phone
    ON users (phone)
    WHERE phone IS NOT NULL;


-- =========================================================
-- ROLES
-- =========================================================

CREATE TABLE roles (
                       id SMALLSERIAL PRIMARY KEY,

                       code VARCHAR(30) NOT NULL UNIQUE,
                       name VARCHAR(60) NOT NULL UNIQUE
);


CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id SMALLINT NOT NULL,

                            PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id)
                                    REFERENCES roles(id)
                                    ON DELETE CASCADE
);


INSERT INTO roles (code, name)
VALUES
    ('PLAYER', 'Player'),
    ('SCORER', 'Scorer'),
    ('ORGANIZER', 'Tournament Organizer'),
    ('ADMIN', 'Administrator');


-- =========================================================
-- PLAYER CATEGORIES
-- =========================================================

CREATE TABLE player_categories (
                                   id SMALLSERIAL PRIMARY KEY,

                                   code VARCHAR(40) NOT NULL UNIQUE,
                                   name VARCHAR(80) NOT NULL UNIQUE,

                                   active BOOLEAN NOT NULL DEFAULT TRUE,

                                   created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


INSERT INTO player_categories (code, name)
VALUES
    ('BATSMAN', 'Batsman'),
    ('BOWLER', 'Bowler'),
    ('ALL_ROUNDER', 'All-rounder'),
    ('WICKETKEEPER', 'Wicketkeeper');


-- =========================================================
-- PLAYERS
-- =========================================================

CREATE TABLE players (
                         id BIGSERIAL PRIMARY KEY,

    -- Nullable because organizers may register players
    -- who do not yet have website accounts.
                         user_id BIGINT,

                         full_name VARCHAR(150) NOT NULL,
                         photo_url TEXT,

                         date_of_birth DATE,

                         primary_category_id SMALLINT,

                         batting_style VARCHAR(50),
                         bowling_style VARCHAR(80),

                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_players_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(id),

                         CONSTRAINT fk_players_primary_category
                             FOREIGN KEY (primary_category_id)
                                 REFERENCES player_categories(id),

                         CONSTRAINT uq_players_user UNIQUE (user_id)
);


CREATE INDEX idx_players_full_name
    ON players(full_name);


-- =========================================================
-- TOURNAMENT PLAYER REGISTRATION
-- =========================================================

CREATE TABLE player_registrations (
                                      id BIGSERIAL PRIMARY KEY,

                                      tournament_edition_id BIGINT NOT NULL,
                                      player_id BIGINT NOT NULL,
                                      category_id SMALLINT NOT NULL,

                                      fee_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,

                                      status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                                      registered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      approved_at TIMESTAMPTZ,
                                      approved_by_user_id BIGINT,

                                      notes TEXT,

                                      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_player_registration_edition
                                          FOREIGN KEY (tournament_edition_id)
                                              REFERENCES tournament_editions(id),

                                      CONSTRAINT fk_player_registration_player
                                          FOREIGN KEY (player_id)
                                              REFERENCES players(id),

                                      CONSTRAINT fk_player_registration_category
                                          FOREIGN KEY (category_id)
                                              REFERENCES player_categories(id),

                                      CONSTRAINT fk_player_registration_approved_by
                                          FOREIGN KEY (approved_by_user_id)
                                              REFERENCES users(id),

                                      CONSTRAINT uq_player_registration
                                          UNIQUE (tournament_edition_id, player_id),

                                      CONSTRAINT chk_registration_fee
                                          CHECK (fee_amount >= 0),

                                      CONSTRAINT chk_registration_status
                                          CHECK (
                                              status IN (
                                                         'PENDING',
                                                         'APPROVED',
                                                         'REJECTED',
                                                         'WITHDRAWN'
                                                  )
                                              )
);


CREATE INDEX idx_player_registrations_edition
    ON player_registrations(tournament_edition_id);

CREATE INDEX idx_player_registrations_player
    ON player_registrations(player_id);

CREATE INDEX idx_player_registrations_status
    ON player_registrations(status);


-- =========================================================
-- REGISTRATION PAYMENTS
-- =========================================================

CREATE TABLE registration_payments (
                                       id BIGSERIAL PRIMARY KEY,

                                       registration_id BIGINT NOT NULL,

                                       amount NUMERIC(10, 2) NOT NULL,

                                       payment_method VARCHAR(30) NOT NULL,

                                       transaction_reference VARCHAR(150),

                                       status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                                       paid_at TIMESTAMPTZ,

                                       verified_at TIMESTAMPTZ,
                                       verified_by_user_id BIGINT,

                                       notes TEXT,

                                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_registration_payment_registration
                                           FOREIGN KEY (registration_id)
                                               REFERENCES player_registrations(id),

                                       CONSTRAINT fk_registration_payment_verified_by
                                           FOREIGN KEY (verified_by_user_id)
                                               REFERENCES users(id),

                                       CONSTRAINT chk_registration_payment_amount
                                           CHECK (amount > 0),

                                       CONSTRAINT chk_registration_payment_status
                                           CHECK (
                                               status IN (
                                                          'PENDING',
                                                          'VERIFIED',
                                                          'REJECTED',
                                                          'REFUNDED'
                                                   )
                                               ),

                                       CONSTRAINT chk_payment_method
                                           CHECK (
                                               payment_method IN (
                                                                  'CASH',
                                                                  'BKASH',
                                                                  'NAGAD',
                                                                  'BANK',
                                                                  'OTHER'
                                                   )
                                               )
);


CREATE INDEX idx_registration_payments_registration
    ON registration_payments(registration_id);

CREATE INDEX idx_registration_payments_status
    ON registration_payments(status);