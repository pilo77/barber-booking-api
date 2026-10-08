ALTER TABLE subscription_payment_orders
    ADD COLUMN payment_method VARCHAR(10) NOT NULL DEFAULT 'WOMPI' CHECK (payment_method IN ('WOMPI', 'MANUAL')),
    ADD COLUMN declared_transfer_reference VARCHAR(80),
    ADD COLUMN reviewed_by BIGINT REFERENCES user_accounts(id),
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN rejection_reason VARCHAR(255);
ALTER TABLE subscription_payment_orders DROP CONSTRAINT subscription_payment_orders_status_check;
ALTER TABLE subscription_payment_orders DROP CONSTRAINT chk_subscription_payment_confirmation;
ALTER TABLE subscription_payment_orders ADD CONSTRAINT chk_subscription_order_status CHECK (status IN ('PENDING', 'PAID', 'REJECTED'));
ALTER TABLE subscription_payment_orders ADD CONSTRAINT chk_subscription_payment_confirmation CHECK (
    (status IN ('PENDING', 'REJECTED') AND provider_transaction_id IS NULL AND paid_at IS NULL)
    OR (status = 'PAID' AND provider_transaction_id IS NOT NULL AND paid_at IS NOT NULL)
);
ALTER TABLE subscription_payment_orders ADD CONSTRAINT chk_manual_review CHECK (
    payment_method <> 'MANUAL'
    OR (declared_transfer_reference IS NOT NULL AND
      ((status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
       OR (status IN ('PAID', 'REJECTED') AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)))
);
CREATE TABLE subscription_payment_reviews (
    id BIGSERIAL PRIMARY KEY,
    order_reference UUID NOT NULL UNIQUE REFERENCES subscription_payment_orders(reference),
    reviewer_id BIGINT NOT NULL REFERENCES user_accounts(id),
    decision VARCHAR(10) NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED')),
    bank_transaction_id VARCHAR(80),
    confirmed_amount_in_cents BIGINT,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (decision <> 'APPROVED' OR (bank_transaction_id IS NOT NULL AND confirmed_amount_in_cents > 0))
);
CREATE UNIQUE INDEX uq_manual_transfer_company_reference
    ON subscription_payment_orders(company_id, declared_transfer_reference) WHERE payment_method = 'MANUAL';
