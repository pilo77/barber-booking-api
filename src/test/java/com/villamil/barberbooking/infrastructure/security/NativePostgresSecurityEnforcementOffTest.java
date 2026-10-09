package com.villamil.barberbooking.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.villamil.barberbooking.application.port.out.JwtTokenPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.support.QaPostgres;

@SpringBootTest(properties = {"spring.config.import=", "app.bootstrap.token=", "billing.enforce-subscription=false"})
@AutoConfigureMockMvc
@EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresSecurityEnforcementOffTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        String name = "qa_enforce_off_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(QaPostgres.dataSource("postgres")).execute("CREATE DATABASE " + name);
        var data = QaPostgres.dataSource(name);
        properties.add("spring.datasource.url", data::getUrl);
        properties.add("spring.datasource.username", data::getUsername);
        properties.add("spring.datasource.password", data::getPassword);
        properties.add("app.jwt.secret", () -> UUID.randomUUID().toString() + UUID.randomUUID());
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired UserAccountRepositoryPort users;
    @Autowired JwtTokenPort jwt;
    long company, branch, owner;

    @BeforeEach void seed() {
        String suffix = UUID.randomUUID().toString();
        company = jdbc.queryForObject("INSERT INTO companies(name,slug) VALUES ('QA Free Company',?) RETURNING id", Long.class, "qa-" + suffix);
        branch = jdbc.queryForObject("INSERT INTO branches(company_id,name,slug) VALUES (?,'QA Branch','principal') RETURNING id", Long.class, company);
        owner = jdbc.queryForObject("""
                INSERT INTO user_accounts(company_id,branch_id,email,full_name,password_hash)
                VALUES (?, ?, ?, 'QA Owner', ?) RETURNING id
                """, Long.class, company, branch, "owner-" + suffix + "@example.test", encoder.encode("qa-only-password-123!"));
        jdbc.update("INSERT INTO user_account_roles(user_account_id,role) VALUES (?,'COMPANY_OWNER')", owner);
        jdbc.update("INSERT INTO company_subscriptions(company_id) VALUES (?)", company);
        jdbc.update("INSERT INTO marketplace_branch_profiles(company_id,branch_id) VALUES (?,?)", company, branch);
    }

    String bearer() { return "Bearer " + jwt.createAccessToken(users.findById(owner).orElseThrow()); }

    @Test void freeOwnerCannotManageTeamEvenWhenLegacyEnforcementIsOff() throws Exception {
        mvc.perform(get("/api/v1/user-accounts").header("Authorization", bearer()))
                .andExpect(status().isPaymentRequired()).andExpect(jsonPath("$.code").value("SUBSCRIPTION_REQUIRED"));
        mvc.perform(get("/api/v1/customers").header("Authorization", bearer())).andExpect(status().isOk());
    }

    @Test void companySuspensionBlocksOperationAndPublicProfileChangesWithEnforcementOff() throws Exception {
        jdbc.update("UPDATE companies SET active=false WHERE id=?", company);
        mvc.perform(get("/api/v1/appointments").header("Authorization", bearer()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMPANY_SUSPENDED"));
        mvc.perform(put("/api/v1/company/marketplace-profile").header("Authorization", bearer())
                .contentType("application/json").content("{\"city\":\"Neiva\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMPANY_SUSPENDED"));
    }

    @Test void branchSuspensionBlocksOperationsWithoutBlockingBilling() throws Exception {
        jdbc.update("UPDATE branches SET active=false WHERE id=?", branch);
        mvc.perform(get("/api/v1/services").header("Authorization", bearer()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMPANY_SUSPENDED"));
        mvc.perform(get("/api/v1/billing/subscription").header("Authorization", bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/company/capabilities").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.companyActive").value(false));
    }

    @Test void validBusinessSubscriptionEnablesTeamManagementRegardlessOfLegacyFlag() throws Exception {
        jdbc.update("UPDATE company_subscriptions SET valid_until=? WHERE company_id=?",
                java.sql.Timestamp.from(Instant.now().plusSeconds(3600)), company);
        mvc.perform(get("/api/v1/user-accounts").header("Authorization", bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/company/capabilities").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tier").value("BUSINESS"))
                .andExpect(jsonPath("$.teamManagement").value(true));
    }
}
