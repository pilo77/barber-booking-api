DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
            AND table_name = 'appointments'
            AND column_name = 'service_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
            AND table_name = 'appointments'
            AND column_name = 'service_offering_id'
    ) THEN
        ALTER TABLE appointments RENAME COLUMN service_id TO service_offering_id;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers (id),
    barber_id BIGINT NOT NULL REFERENCES barbers (id),
    service_offering_id BIGINT NOT NULL REFERENCES services (id),
    start_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL,
    source VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_appointments_range CHECK (start_at < end_at),
    CONSTRAINT chk_appointments_status CHECK (
        status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
    ),
    CONSTRAINT chk_appointments_source CHECK (
        source IN ('ONLINE', 'WALK_IN', 'BARBER_CREATED')
    )
);

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_appointments_customer_id
    ON appointments (customer_id);

CREATE INDEX IF NOT EXISTS idx_appointments_status
    ON appointments (status);
