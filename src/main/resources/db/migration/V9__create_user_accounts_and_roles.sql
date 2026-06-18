CREATE TABLE user_accounts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT,
    branch_id BIGINT,
    email VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_accounts_email UNIQUE (email),
    CONSTRAINT fk_user_accounts_company
        FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_user_accounts_branch_company
        FOREIGN KEY (branch_id, company_id) REFERENCES branches (id, company_id),
    CONSTRAINT chk_user_accounts_email_lower
        CHECK (email = lower(email)),
    CONSTRAINT chk_user_accounts_company_branch_pair
        CHECK (
            (company_id IS NULL AND branch_id IS NULL)
            OR (company_id IS NOT NULL AND branch_id IS NOT NULL)
        )
);

CREATE TABLE user_account_roles (
    user_account_id BIGINT NOT NULL,
    role VARCHAR(40) NOT NULL,
    CONSTRAINT pk_user_account_roles PRIMARY KEY (user_account_id, role),
    CONSTRAINT fk_user_account_roles_user
        FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_account_roles_role CHECK (
        role IN (
            'PLATFORM_OWNER',
            'COMPANY_OWNER',
            'BRANCH_MANAGER',
            'RECEPTIONIST',
            'BARBER',
            'CASHIER',
            'ACCOUNTANT',
            'INVENTORY_MANAGER',
            'CUSTOMER'
        )
    )
);

CREATE INDEX idx_user_accounts_email ON user_accounts (email);
CREATE INDEX idx_user_accounts_company ON user_accounts (company_id);
CREATE INDEX idx_user_accounts_branch ON user_accounts (branch_id);
CREATE INDEX idx_user_accounts_active ON user_accounts (active);
CREATE INDEX idx_user_account_roles_role ON user_account_roles (role);
