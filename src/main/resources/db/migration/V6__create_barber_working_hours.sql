CREATE TABLE IF NOT EXISTS barber_working_hours (
    id BIGSERIAL PRIMARY KEY,
    barber_id BIGINT NOT NULL REFERENCES barbers (id),
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_barber_working_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_barber_working_hours_range CHECK (start_time < end_time)
);

ALTER TABLE barber_working_hours
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE barber_working_hours
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_barber_working_hours_barber
    ON barber_working_hours (barber_id);

CREATE INDEX IF NOT EXISTS idx_barber_working_hours_barber_day_active
    ON barber_working_hours (barber_id, day_of_week, active);
