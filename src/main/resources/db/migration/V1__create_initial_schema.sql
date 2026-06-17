CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_customers_phone UNIQUE (phone),
    CONSTRAINT uq_customers_email UNIQUE (email)
);

CREATE TABLE barbers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE barber_services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_barber_services_name UNIQUE (name),
    CONSTRAINT chk_barber_services_duration_positive CHECK (duration_minutes > 0),
    CONSTRAINT chk_barber_services_price_non_negative CHECK (price >= 0)
);

CREATE TABLE barber_working_hours (
    id BIGSERIAL PRIMARY KEY,
    barber_id BIGINT NOT NULL REFERENCES barbers (id),
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_barber_working_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_barber_working_hours_range CHECK (start_time < end_time)
);

CREATE INDEX idx_barber_working_hours_barber_day
    ON barber_working_hours (barber_id, day_of_week);

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers (id),
    barber_id BIGINT NOT NULL REFERENCES barbers (id),
    service_id BIGINT NOT NULL REFERENCES barber_services (id),
    start_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL,
    source VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_appointments_range CHECK (start_at < end_at),
    CONSTRAINT chk_appointments_status CHECK (
        status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
    ),
    CONSTRAINT chk_appointments_source CHECK (
        source IN ('ONLINE', 'WALK_IN', 'BARBER_CREATED')
    )
);

CREATE INDEX idx_appointments_barber_start_at
    ON appointments (barber_id, start_at);

CREATE INDEX idx_appointments_customer_start_at
    ON appointments (customer_id, start_at);

ALTER TABLE appointments
    ADD CONSTRAINT ex_appointments_no_active_overlap
    EXCLUDE USING gist (
        barber_id WITH =,
        tsrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status IN ('SCHEDULED', 'IN_PROGRESS'));
