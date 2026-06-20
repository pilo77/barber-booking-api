ALTER TABLE appointments
    ADD CONSTRAINT uq_appointments_id_company_branch
        UNIQUE (id, company_id, branch_id);

CREATE TABLE public_booking_idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    appointment_id BIGINT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_public_booking_idempotency_tenant_key
        UNIQUE (company_id, branch_id, idempotency_key),
    CONSTRAINT fk_public_booking_idempotency_branch_tenant
        FOREIGN KEY (branch_id, company_id) REFERENCES branches (id, company_id),
    CONSTRAINT fk_public_booking_idempotency_appointment_tenant
        FOREIGN KEY (appointment_id, company_id, branch_id)
        REFERENCES appointments (id, company_id, branch_id),
    CONSTRAINT chk_public_booking_idempotency_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT chk_public_booking_idempotency_completion
        CHECK (
            (status = 'IN_PROGRESS' AND appointment_id IS NULL AND completed_at IS NULL)
            OR
            (status = 'COMPLETED' AND appointment_id IS NOT NULL AND completed_at IS NOT NULL)
        )
);

CREATE INDEX idx_public_booking_idempotency_created_at
    ON public_booking_idempotency_keys (created_at);
