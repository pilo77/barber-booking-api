package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.villamil.barberbooking.application.port.out.PlatformOwnerProvisioningPort;

@Component
public class PlatformOwnerProvisioningAdapter implements PlatformOwnerProvisioningPort {
    private final JdbcTemplate jdbc;

    public PlatformOwnerProvisioningAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean createIfAbsent(String email, String fullName, String passwordHash) {
        // Serializes first-owner creation across concurrent deployment instances.
        jdbc.execute("SELECT pg_advisory_xact_lock(719104386)");
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM user_account_roles WHERE role = 'PLATFORM_OWNER')", Boolean.class))) {
            return false;
        }
        // An existing company account is never promoted or given a new password.
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM user_accounts WHERE email = ?)", Boolean.class, email))) {
            throw new IllegalStateException("Platform owner email is already assigned; no account was modified");
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO user_accounts(email, full_name, password_hash)
                VALUES (?, ?, ?) RETURNING id
                """, Long.class, email, fullName, passwordHash);
        jdbc.update("INSERT INTO user_account_roles(user_account_id, role) VALUES (?, 'PLATFORM_OWNER')", id);
        return true;
    }
}
