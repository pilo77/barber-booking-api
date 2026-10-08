-- Preserve historical orders; new manual reports identify the authenticated reporter.
ALTER TABLE user_accounts ADD CONSTRAINT uq_user_accounts_id_company UNIQUE (id, company_id);
ALTER TABLE subscription_payment_orders ADD COLUMN reported_by_user_id BIGINT;
ALTER TABLE subscription_payment_orders ADD CONSTRAINT fk_payment_reporter_company
    FOREIGN KEY (reported_by_user_id, company_id) REFERENCES user_accounts (id, company_id);
