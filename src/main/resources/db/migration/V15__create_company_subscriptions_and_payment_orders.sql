CREATE TABLE company_subscriptions (
    company_id BIGINT PRIMARY KEY REFERENCES companies (id),
    plan VARCHAR(20) NOT NULL DEFAULT 'BASIC' CHECK (plan = 'BASIC'),
    valid_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Existing tenants require an explicit verified payment; no fabricated payment or trial.
INSERT INTO company_subscriptions (company_id) SELECT id FROM companies;

CREATE TABLE subscription_payment_orders (
    reference UUID PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company_subscriptions (company_id),
    idempotency_key VARCHAR(128) NOT NULL,
    amount_in_cents BIGINT NOT NULL CHECK (amount_in_cents > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'COP'),
    environment VARCHAR(4) NOT NULL CHECK (environment IN ('test', 'prod')),
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID')),
    provider_transaction_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at TIMESTAMPTZ,
    CONSTRAINT uq_subscription_checkout_company_key UNIQUE (company_id, idempotency_key),
    CONSTRAINT uq_subscription_provider_transaction UNIQUE (environment, provider_transaction_id),
    CONSTRAINT chk_subscription_payment_confirmation CHECK (
        (status = 'PENDING' AND provider_transaction_id IS NULL AND paid_at IS NULL)
        OR (status = 'PAID' AND provider_transaction_id IS NOT NULL AND paid_at IS NOT NULL)
    )
);
CREATE INDEX idx_subscription_orders_company_created ON subscription_payment_orders(company_id, created_at);
