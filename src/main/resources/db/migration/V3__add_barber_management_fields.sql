ALTER TABLE barbers
    ADD COLUMN email VARCHAR(120);

ALTER TABLE barbers
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE barbers
    ALTER COLUMN phone SET NOT NULL;

ALTER TABLE barbers
    ADD CONSTRAINT uq_barbers_phone UNIQUE (phone);

ALTER TABLE barbers
    ADD CONSTRAINT uq_barbers_email UNIQUE (email);

CREATE INDEX idx_barbers_active
    ON barbers (active);
