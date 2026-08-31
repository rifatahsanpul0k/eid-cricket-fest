ALTER TABLE player_registrations
    ADD COLUMN rejected_at TIMESTAMPTZ,
    ADD COLUMN rejected_by_user_id BIGINT,
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE player_registrations
    ADD CONSTRAINT fk_registration_rejected_by
        FOREIGN KEY (rejected_by_user_id)
            REFERENCES users(id);


ALTER TABLE registration_payments
    ADD COLUMN submitted_by_user_id BIGINT,
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE registration_payments
    ADD CONSTRAINT fk_payment_submitted_by
        FOREIGN KEY (submitted_by_user_id)
            REFERENCES users(id);


-- Digital payments must have a transaction reference.
ALTER TABLE registration_payments
    ADD CONSTRAINT chk_digital_payment_reference
        CHECK (
            payment_method NOT IN ('BKASH', 'NAGAD', 'BANK')
                OR (
                transaction_reference IS NOT NULL
                    AND LENGTH(TRIM(transaction_reference)) > 0
                )
            );


-- Prevent the same transaction from being submitted twice.
CREATE UNIQUE INDEX uq_payment_transaction_reference
    ON registration_payments (
                              payment_method,
                              transaction_reference
        )
    WHERE transaction_reference IS NOT NULL;