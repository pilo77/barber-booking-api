CREATE TABLE marketplace_branch_profiles (
    branch_id BIGINT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    publication_state VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    city VARCHAR(120),
    sector VARCHAR(120),
    address VARCHAR(240),
    description VARCHAR(2000),
    contact_phone VARCHAR(30),
    cover_image_url VARCHAR(500),
    submitted_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    reviewed_by BIGINT REFERENCES user_accounts(id),
    review_reason VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_marketplace_profile_tenant UNIQUE (branch_id, company_id),
    CONSTRAINT fk_marketplace_profile_branch FOREIGN KEY (branch_id, company_id) REFERENCES branches(id, company_id),
    CONSTRAINT chk_marketplace_publication_state CHECK (publication_state IN ('DRAFT','PENDING_REVIEW','PUBLISHED','HIDDEN','SUSPENDED')),
    CONSTRAINT chk_marketplace_published_complete CHECK (publication_state <> 'PUBLISHED' OR
        (length(trim(city)) > 0 AND city IS NOT NULL AND length(trim(address)) > 0 AND address IS NOT NULL
         AND length(trim(description)) > 0 AND description IS NOT NULL AND length(trim(contact_phone)) > 0 AND contact_phone IS NOT NULL
         AND cover_image_url IS NOT NULL AND cover_image_url LIKE 'https://%' AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL))
);

CREATE INDEX idx_marketplace_publication_city ON marketplace_branch_profiles(publication_state, lower(city), branch_id);
CREATE INDEX idx_marketplace_pending_review ON marketplace_branch_profiles(submitted_at, branch_id) WHERE publication_state = 'PENDING_REVIEW';

CREATE TABLE marketplace_publication_events (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL REFERENCES user_accounts(id),
    actor_company_id BIGINT,
    from_state VARCHAR(30) NOT NULL,
    to_state VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_marketplace_event_profile FOREIGN KEY (branch_id, company_id) REFERENCES marketplace_branch_profiles(branch_id, company_id),
    CONSTRAINT fk_marketplace_event_actor_tenant FOREIGN KEY (actor_id, actor_company_id) REFERENCES user_accounts(id, company_id),
    CONSTRAINT chk_marketplace_event_actor_company CHECK (actor_company_id IS NULL OR actor_company_id = company_id),
    CONSTRAINT chk_marketplace_event_from_state CHECK (from_state IN ('DRAFT','PENDING_REVIEW','PUBLISHED','HIDDEN','SUSPENDED')),
    CONSTRAINT chk_marketplace_event_to_state CHECK (to_state IN ('DRAFT','PENDING_REVIEW','PUBLISHED','HIDDEN','SUSPENDED'))
);
CREATE INDEX idx_marketplace_events_tenant ON marketplace_publication_events(company_id, branch_id, created_at);
