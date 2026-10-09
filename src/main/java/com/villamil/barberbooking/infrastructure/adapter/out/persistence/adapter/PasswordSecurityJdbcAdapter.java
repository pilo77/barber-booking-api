package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.util.OptionalLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.villamil.barberbooking.application.port.out.PasswordSecurityRepositoryPort;
import com.villamil.barberbooking.application.port.out.SessionVersionRepositoryPort;

@Component
public class PasswordSecurityJdbcAdapter implements PasswordSecurityRepositoryPort, SessionVersionRepositoryPort {
    private final JdbcTemplate jdbc;

    public PasswordSecurityJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long currentVersion(Long userId) {
        return jdbc.query("""
                SELECT session_version FROM user_account_security_versions WHERE user_account_id = ?
                """, (rs, row) -> rs.getLong("session_version"), userId).stream().findFirst().orElse(0L);
    }

    @Override
    public OptionalLong currentVersionForPasswordSnapshot(Long userId, String passwordHash) {
        var versions = jdbc.query("""
                SELECT COALESCE(v.session_version, 0) AS session_version
                FROM user_accounts u
                LEFT JOIN user_account_security_versions v ON v.user_account_id = u.id
                WHERE u.id = ? AND u.active = true AND u.password_hash = ?
                """, (rs, row) -> rs.getLong("session_version"), userId, passwordHash);
        return versions.isEmpty() ? OptionalLong.empty() : OptionalLong.of(versions.getFirst());
    }

    @Override
    @Transactional
    public boolean compareAndChangePassword(Long userId, String expectedPasswordHash, String newPasswordHash) {
        int changed = jdbc.update("""
                UPDATE user_accounts SET password_hash = ?, updated_at = now()
                WHERE id = ? AND active = true AND password_hash = ?
                """, newPasswordHash, userId, expectedPasswordHash);
        if (changed == 0) {
            return false;
        }
        jdbc.update("""
                INSERT INTO user_account_security_versions(user_account_id, session_version)
                VALUES (?, 1)
                ON CONFLICT (user_account_id) DO UPDATE
                SET session_version = user_account_security_versions.session_version + 1
                """, userId);
        return true;
    }
}
