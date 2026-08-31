ALTER TABLE tournament_editions
    ADD COLUMN registration_fee NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN registration_currency VARCHAR(3) NOT NULL DEFAULT 'BDT';

ALTER TABLE tournament_editions
    ADD CONSTRAINT chk_tournament_registration_fee
        CHECK (registration_fee >= 0),

    ADD CONSTRAINT chk_registration_currency
        CHECK (
            CHAR_LENGTH(registration_currency) = 3
                AND registration_currency = UPPER(registration_currency)
            );