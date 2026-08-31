-- =========================================================
-- DELIVERY IDEMPOTENCY
-- =========================================================

ALTER TABLE deliveries
    ADD COLUMN client_event_id UUID;

CREATE UNIQUE INDEX uq_delivery_client_event_id
    ON deliveries(client_event_id)
    WHERE client_event_id IS NOT NULL;


-- =========================================================
-- COMPLETE DISMISSAL MODEL
-- =========================================================

ALTER TABLE wickets
    DROP CONSTRAINT chk_dismissal_type;

ALTER TABLE wickets
    ADD CONSTRAINT chk_dismissal_type
        CHECK (
            dismissal_type IN (
                'BOWLED',
                'CAUGHT',
                'LBW',
                'RUN_OUT',
                'STUMPED',
                'HIT_WICKET',
                'HIT_BALL_TWICE',
                'OBSTRUCTING_FIELD'
            )
        );
