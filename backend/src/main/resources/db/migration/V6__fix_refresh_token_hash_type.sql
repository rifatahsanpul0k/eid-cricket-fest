ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64)
        USING RTRIM(token_hash);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT chk_refresh_token_hash_length
        CHECK (CHAR_LENGTH(token_hash) = 64);