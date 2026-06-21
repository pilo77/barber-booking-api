CREATE TABLE company_public_profiles (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies (id),
    public_name VARCHAR(120),
    public_description VARCHAR(1000),
    logo_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    accent_color VARCHAR(7),
    theme_mode VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    contact_phone VARCHAR(30),
    contact_whatsapp_url VARCHAR(500),
    contact_instagram_url VARCHAR(500),
    contact_facebook_url VARCHAR(500),
    contact_tiktok_url VARCHAR(500),
    contact_website_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    CONSTRAINT uq_company_public_profiles_company UNIQUE (company_id),
    CONSTRAINT chk_company_public_profiles_theme_mode CHECK (theme_mode IN ('LIGHT', 'DARK', 'SYSTEM')),
    CONSTRAINT chk_company_public_profiles_primary_color
        CHECK (primary_color IS NULL OR primary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_company_public_profiles_secondary_color
        CHECK (secondary_color IS NULL OR secondary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_company_public_profiles_accent_color
        CHECK (accent_color IS NULL OR accent_color ~ '^#[0-9A-Fa-f]{6}$')
);

INSERT INTO company_public_profiles (
    company_id,
    public_name,
    public_description,
    logo_url,
    theme_mode,
    created_at,
    updated_at
)
SELECT
    id,
    name,
    description,
    logo_url,
    'SYSTEM',
    CURRENT_TIMESTAMP AT TIME ZONE 'UTC',
    CURRENT_TIMESTAMP AT TIME ZONE 'UTC'
FROM companies;
