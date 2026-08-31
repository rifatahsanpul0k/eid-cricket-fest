ALTER TABLE deliveries
    ADD COLUMN undo_client_event_id UUID;

CREATE UNIQUE INDEX uq_deliveries_undo_client_event_id
    ON deliveries (undo_client_event_id)
    WHERE undo_client_event_id IS NOT NULL;

ALTER TABLE innings
    ADD COLUMN score_revision BIGINT NOT NULL DEFAULT 0;
