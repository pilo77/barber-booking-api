DO $$
BEGIN
    IF to_regclass('public.services') IS NULL
        AND to_regclass('public.barber_services') IS NOT NULL THEN
        ALTER TABLE barber_services RENAME TO services;
    END IF;
END $$;

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS description VARCHAR(255);

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE services
    ALTER COLUMN name TYPE VARCHAR(120);

ALTER TABLE services
    ALTER COLUMN price TYPE NUMERIC(12, 2);

CREATE INDEX IF NOT EXISTS idx_services_active
    ON services (active);
