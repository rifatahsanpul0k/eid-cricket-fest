ALTER TABLE matches
    DROP CONSTRAINT chk_match_status;

ALTER TABLE matches
    ADD CONSTRAINT chk_match_status
        CHECK (
            status IN (
                'PLANNED',
                'SCHEDULED',
                'READY',
                'TOSS_COMPLETED',
                'LIVE',
                'INNINGS_BREAK',
                'SUSPENDED',
                'COMPLETED',
                'POSTPONED',
                'ABANDONED',
                'CANCELLED'
            )
        );

ALTER TABLE matches
    ADD COLUMN result_status VARCHAR(30),
    ADD COLUMN rematch_of_match_id BIGINT,
    ADD COLUMN superseded_by_match_id BIGINT,
    ADD COLUMN suspended_from_status VARCHAR(30);

UPDATE matches
SET result_status = 'OFFICIAL'
WHERE status = 'COMPLETED'
  AND result_type IS NOT NULL;

ALTER TABLE matches
    ADD CONSTRAINT fk_match_rematch_of
        FOREIGN KEY (rematch_of_match_id)
        REFERENCES matches(id),
    ADD CONSTRAINT fk_match_superseded_by
        FOREIGN KEY (superseded_by_match_id)
        REFERENCES matches(id),
    ADD CONSTRAINT chk_match_result_status
        CHECK (
            result_status IS NULL
            OR result_status IN (
                'OFFICIAL',
                'UNDER_REVIEW',
                'VOID',
                'SUPERSEDED'
            )
        ),
    ADD CONSTRAINT chk_match_suspended_from_status
        CHECK (
            suspended_from_status IS NULL
            OR suspended_from_status IN (
                'TOSS_COMPLETED',
                'LIVE',
                'INNINGS_BREAK'
            )
        );

CREATE INDEX idx_matches_result_status
    ON matches(result_status);

CREATE INDEX idx_matches_rematch_of
    ON matches(rematch_of_match_id);

CREATE INDEX idx_matches_superseded_by
    ON matches(superseded_by_match_id);

CREATE TABLE match_operation_audits (
    id BIGSERIAL PRIMARY KEY,

    match_id BIGINT NOT NULL,
    operation_type VARCHAR(40) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,

    old_status VARCHAR(30),
    new_status VARCHAR(30),
    old_result_status VARCHAR(30),
    new_result_status VARCHAR(30),
    metadata TEXT,
    related_match_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_match_operation_match
        FOREIGN KEY (match_id)
        REFERENCES matches(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_match_operation_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_match_operation_related_match
        FOREIGN KEY (related_match_id)
        REFERENCES matches(id),

    CONSTRAINT chk_match_operation_type
        CHECK (
            operation_type IN (
                'RESCHEDULE',
                'POSTPONE',
                'SUSPEND',
                'RESUME',
                'ABANDON',
                'CANCEL',
                'RESET_TOSS',
                'MARK_UNDER_REVIEW',
                'RESTORE_OFFICIAL',
                'VOID_RESULT',
                'ORDER_REMATCH'
            )
        ),

    CONSTRAINT chk_match_operation_reason
        CHECK (length(trim(reason)) > 0)
);

CREATE INDEX idx_match_operation_audits_match
    ON match_operation_audits(match_id, created_at DESC);
