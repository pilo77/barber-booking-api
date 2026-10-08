package com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.villamil.barberbooking.application.dto.response.CompanyContextResponse;
import com.villamil.barberbooking.application.port.out.CompanyRegistrationPort;

@Component
public class CompanyRegistrationAdapter implements CompanyRegistrationPort {
    private final JdbcTemplate jdbc;
    public CompanyRegistrationAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public CompanyContextResponse create(String name, String slug, String branchName,
            String ownerName, String email, String passwordHash) {
        Long company = jdbc.queryForObject("INSERT INTO companies(name, slug) VALUES (?, ?) RETURNING id", Long.class, name, slug);
        Long branch = jdbc.queryForObject("INSERT INTO branches(company_id, name, slug) VALUES (?, ?, 'principal') RETURNING id",
                Long.class, company, branchName);
        Long user = jdbc.queryForObject("""
                INSERT INTO user_accounts(company_id, branch_id, email, password_hash, full_name)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """, Long.class, company, branch, email, passwordHash, ownerName);
        jdbc.update("INSERT INTO user_account_roles(user_account_id, role) VALUES (?, 'COMPANY_OWNER')", user);
        jdbc.update("INSERT INTO company_subscriptions(company_id) VALUES (?)", company);
        return new CompanyContextResponse(name, slug, branchName, "principal");
    }
    @Override public CompanyContextResponse context(Long companyId, Long branchId) {
        return jdbc.queryForObject("""
                SELECT c.name AS company_name, c.slug AS company_slug, b.name AS branch_name, b.slug AS branch_slug
                FROM companies c JOIN branches b ON b.company_id = c.id WHERE c.id = ? AND b.id = ?
                """, (rs, row) -> new CompanyContextResponse(rs.getString("company_name"), rs.getString("company_slug"),
                        rs.getString("branch_name"), rs.getString("branch_slug")), companyId, branchId);
    }
}
