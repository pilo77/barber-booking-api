ALTER TABLE customers
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE customers
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX idx_customers_active
    ON customers (active);
