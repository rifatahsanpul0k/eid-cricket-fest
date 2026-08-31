CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,

                                user_id BIGINT NOT NULL,

                                token_hash CHAR(64) NOT NULL,
                                family_id UUID NOT NULL,

                                expires_at TIMESTAMPTZ NOT NULL,
                                revoked_at TIMESTAMPTZ,

                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_refresh_token_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT uq_refresh_token_hash
                                    UNIQUE (token_hash),

                                CONSTRAINT chk_refresh_token_expiration
                                    CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_tokens_user
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_family
    ON refresh_tokens(family_id);

CREATE INDEX idx_active_refresh_tokens
    ON refresh_tokens(expires_at)
    WHERE revoked_at IS NULL;