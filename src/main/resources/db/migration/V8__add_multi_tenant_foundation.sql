CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_companies_slug UNIQUE (slug)
);

CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies (id),
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_branches_company_slug UNIQUE (company_id, slug)
);

INSERT INTO companies (id, name, slug, active)
VALUES (1, 'Default Barber Company', 'default-company', true);

INSERT INTO branches (id, company_id, name, slug, address, phone, active)
VALUES (1, 1, 'Default Branch', 'default-branch', NULL, NULL, true);

SELECT setval(pg_get_serial_sequence('companies', 'id'), (SELECT MAX(id) FROM companies));
SELECT setval(pg_get_serial_sequence('branches', 'id'), (SELECT MAX(id) FROM branches));

ALTER TABLE customers
    ADD COLUMN company_id BIGINT;

UPDATE customers
SET company_id = 1
WHERE company_id IS NULL;

ALTER TABLE customers
    ALTER COLUMN company_id SET NOT NULL,
    ADD CONSTRAINT fk_customers_company FOREIGN KEY (company_id) REFERENCES companies (id);

ALTER TABLE barbers
    ADD COLUMN company_id BIGINT,
    ADD COLUMN branch_id BIGINT;

UPDATE barbers
SET company_id = 1,
    branch_id = 1
WHERE company_id IS NULL
    OR branch_id IS NULL;

ALTER TABLE barbers
    ALTER COLUMN company_id SET NOT NULL,
    ALTER COLUMN branch_id SET NOT NULL,
    ADD CONSTRAINT fk_barbers_company FOREIGN KEY (company_id) REFERENCES companies (id),
    ADD CONSTRAINT fk_barbers_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

ALTER TABLE services
    ADD COLUMN company_id BIGINT;

UPDATE services
SET company_id = 1
WHERE company_id IS NULL;

ALTER TABLE services
    ALTER COLUMN company_id SET NOT NULL,
    ADD CONSTRAINT fk_services_company FOREIGN KEY (company_id) REFERENCES companies (id);

ALTER TABLE barber_working_hours
    ADD COLUMN company_id BIGINT,
    ADD COLUMN branch_id BIGINT;

UPDATE barber_working_hours
SET company_id = 1,
    branch_id = 1
WHERE company_id IS NULL
    OR branch_id IS NULL;

ALTER TABLE barber_working_hours
    ALTER COLUMN company_id SET NOT NULL,
    ALTER COLUMN branch_id SET NOT NULL,
    ADD CONSTRAINT fk_barber_working_hours_company FOREIGN KEY (company_id) REFERENCES companies (id),
    ADD CONSTRAINT fk_barber_working_hours_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

ALTER TABLE appointments
    ADD COLUMN company_id BIGINT,
    ADD COLUMN branch_id BIGINT;

UPDATE appointments
SET company_id = 1,
    branch_id = 1
WHERE company_id IS NULL
    OR branch_id IS NULL;

ALTER TABLE appointments
    ALTER COLUMN company_id SET NOT NULL,
    ALTER COLUMN branch_id SET NOT NULL,
    ADD CONSTRAINT fk_appointments_company FOREIGN KEY (company_id) REFERENCES companies (id),
    ADD CONSTRAINT fk_appointments_branch FOREIGN KEY (branch_id) REFERENCES branches (id);

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS uq_customers_phone,
    DROP CONSTRAINT IF EXISTS uq_customers_email;

ALTER TABLE customers
    ADD CONSTRAINT uq_customers_company_phone UNIQUE (company_id, phone),
    ADD CONSTRAINT uq_customers_company_email UNIQUE (company_id, email);

ALTER TABLE barbers
    DROP CONSTRAINT IF EXISTS uq_barbers_phone,
    DROP CONSTRAINT IF EXISTS uq_barbers_email;

ALTER TABLE barbers
    ADD CONSTRAINT uq_barbers_company_phone UNIQUE (company_id, phone),
    ADD CONSTRAINT uq_barbers_company_email UNIQUE (company_id, email);

ALTER TABLE services
    DROP CONSTRAINT IF EXISTS uq_barber_services_name,
    DROP CONSTRAINT IF EXISTS uq_services_name;

ALTER TABLE services
    ADD CONSTRAINT uq_services_company_name UNIQUE (company_id, name);

ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS ex_appointments_no_active_overlap;

ALTER TABLE appointments
    ADD CONSTRAINT ex_appointments_no_active_overlap
    EXCLUDE USING gist (
        company_id WITH =,
        branch_id WITH =,
        barber_id WITH =,
        tsrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status IN ('SCHEDULED', 'IN_PROGRESS'));

CREATE INDEX idx_customers_company_active
    ON customers (company_id, active);

CREATE INDEX idx_barbers_company_branch_active
    ON barbers (company_id, branch_id, active);

CREATE INDEX idx_services_company_active
    ON services (company_id, active);

CREATE INDEX idx_barber_working_hours_tenant_barber_day_active
    ON barber_working_hours (company_id, branch_id, barber_id, day_of_week, active);

CREATE INDEX idx_appointments_tenant_barber_start_at
    ON appointments (company_id, branch_id, barber_id, start_at);

CREATE INDEX idx_appointments_tenant_status_start_at
    ON appointments (company_id, branch_id, status, start_at);
