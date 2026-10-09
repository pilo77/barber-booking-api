CREATE TABLE user_account_security_versions (
    user_account_id BIGINT PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    session_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_user_account_session_version CHECK (session_version >= 0)
);

-- Existing accounts deliberately begin at version zero without a destructive backfill.
-- Legacy tokens remain valid only until the first successful password change.
