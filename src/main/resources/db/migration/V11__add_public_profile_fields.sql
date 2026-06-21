ALTER TABLE companies
    ADD COLUMN logo_url VARCHAR(500),
    ADD COLUMN description VARCHAR(500);

ALTER TABLE barbers
    ADD COLUMN photo_url VARCHAR(500),
    ADD COLUMN public_display_name VARCHAR(120),
    ADD COLUMN bio VARCHAR(1000),
    ADD COLUMN specialties VARCHAR(255),
    ADD COLUMN active_for_online_booking BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

ALTER TABLE services
    ADD COLUMN visible_for_online_booking BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_companies_public_slug_active
    ON companies (slug, active);

CREATE INDEX idx_branches_public_company_slug_active
    ON branches (company_id, slug, active);

CREATE INDEX idx_barbers_public_listing
    ON barbers (company_id, branch_id, active, active_for_online_booking, sort_order, id);

CREATE INDEX idx_services_public_listing
    ON services (company_id, active, visible_for_online_booking, sort_order, id);
