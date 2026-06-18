ALTER TABLE user_accounts
    ADD COLUMN barber_id BIGINT;

ALTER TABLE user_accounts
    ADD CONSTRAINT fk_user_accounts_barber_tenant
        FOREIGN KEY (barber_id, company_id, branch_id)
        REFERENCES barbers (id, company_id, branch_id);

CREATE UNIQUE INDEX uq_user_accounts_barber_id
    ON user_accounts (barber_id)
    WHERE barber_id IS NOT NULL;

CREATE INDEX idx_user_accounts_barber_id
    ON user_accounts (barber_id);
