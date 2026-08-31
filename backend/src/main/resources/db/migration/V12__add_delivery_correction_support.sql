-- The old unique constraint prevents us from replacing a
-- voided delivery with another delivery using the same sequence number.
ALTER TABLE deliveries
    DROP CONSTRAINT uq_innings_delivery_sequence;


-- Only one ACTIVE version of a delivery sequence may exist.
CREATE UNIQUE INDEX uq_active_innings_delivery_sequence
    ON deliveries (innings_id, sequence_no)
    WHERE voided_at IS NULL;


ALTER TABLE deliveries
    ADD COLUMN correction_of_delivery_id BIGINT,
    ADD COLUMN commentary TEXT;

ALTER TABLE deliveries
    ADD CONSTRAINT fk_delivery_correction_of
        FOREIGN KEY (correction_of_delivery_id)
        REFERENCES deliveries(id);


CREATE INDEX idx_delivery_correction_of
    ON deliveries(correction_of_delivery_id);
