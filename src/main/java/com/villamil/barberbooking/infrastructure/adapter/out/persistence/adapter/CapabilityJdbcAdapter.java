package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.villamil.barberbooking.application.port.out.CapabilityRepositoryPort;
import com.villamil.barberbooking.domain.capabilities.CompanyCapabilityStatus;

@Component
public class CapabilityJdbcAdapter implements CapabilityRepositoryPort {
    private final JdbcTemplate jdbc;

    public CapabilityJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CompanyCapabilityStatus> findStatus(Long companyId, Long branchId) {
        return jdbc.query("""
                SELECT c.active AS company_active, b.active AS branch_active, s.valid_until
                FROM companies c
                JOIN branches b ON b.company_id = c.id AND b.id = ?
                LEFT JOIN company_subscriptions s ON s.company_id = c.id
                WHERE c.id = ?
                """, (rs, row) -> {
                    Timestamp validUntil = rs.getTimestamp("valid_until");
                    return new CompanyCapabilityStatus(rs.getBoolean("company_active"),
                            rs.getBoolean("branch_active"),
                            validUntil == null ? null : validUntil.toInstant());
                }, branchId, companyId).stream().findFirst();
    }

    @Override
    public Optional<String> findPublicationState(Long companyId, Long branchId) {
        return jdbc.query("""
                SELECT publication_state FROM marketplace_branch_profiles
                WHERE company_id = ? AND branch_id = ?
                """, (rs, row) -> rs.getString("publication_state"), companyId, branchId)
                .stream().findFirst();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<String> lockPublicationState(Long companyId, Long branchId) {
        return jdbc.query("""
                SELECT publication_state FROM marketplace_branch_profiles
                WHERE company_id = ? AND branch_id = ? FOR SHARE
                """, (rs, row) -> rs.getString("publication_state"), companyId, branchId)
                .stream().findFirst();
    }
}
