package com.villamil.barberbooking.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villamil.barberbooking.application.port.out.JwtTokenPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.support.QaPostgres;

@SpringBootTest(properties = {"spring.config.import=", "app.bootstrap.token=", "billing.enforce-subscription=true"})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@org.junit.jupiter.api.condition.EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresPasswordSecurityFlowTest {
    private static final String OLD = "qa-existing-password-123!";
    private static final String NEW = "qa-replacement-password-456!";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired UserAccountRepositoryPort users;
    @Autowired JwtTokenPort tokens;
    @Autowired ObjectMapper json;
    private long userId;
    private String email;

    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        String database = "qa_password_flow_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(QaPostgres.dataSource("postgres")).execute("CREATE DATABASE " + database);
        var data = QaPostgres.dataSource(database);
        properties.add("spring.datasource.url", data::getUrl);
        properties.add("spring.datasource.username", data::getUsername);
        properties.add("spring.datasource.password", data::getPassword);
        properties.add("app.jwt.secret", () -> UUID.randomUUID().toString() + UUID.randomUUID());
    }

    @BeforeEach void seedAccount() {
        email = "qa-password-" + UUID.randomUUID() + "@example.test";
        userId = jdbc.queryForObject("""
                INSERT INTO user_accounts(email,password_hash,full_name)
                VALUES (?,?,'QA Platform Owner') RETURNING id
                """, Long.class, email, encoder.encode(OLD));
        jdbc.update("INSERT INTO user_account_roles(user_account_id,role) VALUES (?,'PLATFORM_OWNER')", userId);
    }

    @Test void passwordChangeRevokesExistingTokensAndRequiresNewCredentialForLogin() throws Exception {
        String token = tokens.createAccessToken(users.findById(userId).orElseThrow());
        mvc.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("oldPassword", OLD, "newPassword", NEW))))
                .andExpect(status().isNoContent());
        assertEquals(1L, jdbc.queryForObject("SELECT session_version FROM user_account_security_versions WHERE user_account_id=?",
                Long.class, userId));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", OLD))))
                .andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", NEW))))
                .andExpect(status().isOk()).andReturn();
        String replacementToken = json.readTree(login.getResponse().getContentAsString()).path("accessToken").asText();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + replacementToken))
                .andExpect(status().isOk());
    }

    @Test void incorrectCurrentPasswordDoesNotRevokeCurrentSession() throws Exception {
        String token = tokens.createAccessToken(users.findById(userId).orElseThrow());
        mvc.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("oldPassword", "incorrect-current", "newPassword", NEW))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unable to change password with the supplied data"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user_account_security_versions WHERE user_account_id=?",
                Integer.class, userId));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test void unauthenticatedPasswordChangeIsRejectedBeforeService() throws Exception {
        mvc.perform(post("/api/v1/auth/change-password").contentType("application/json")
                .content(json.writeValueAsString(Map.of("oldPassword", OLD, "newPassword", NEW))))
                .andExpect(status().isUnauthorized());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user_account_security_versions WHERE user_account_id=?",
                Integer.class, userId));
    }

    @Test void staleJpaActivationCannotRestoreHashOrResurrectRevokedSession() throws Exception {
        var stale = users.findById(userId).orElseThrow();
        String oldToken = tokens.createAccessToken(stale);
        mvc.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer " + oldToken)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("oldPassword", OLD, "newPassword", NEW))))
                .andExpect(status().isNoContent());
        // Both activation writes deliberately carry a stale password snapshot through the JPA adapter.
        users.save(stale.deactivate());
        users.save(stale);
        String persistedHash = jdbc.queryForObject("SELECT password_hash FROM user_accounts WHERE id=?", String.class, userId);
        assertTrue(encoder.matches(NEW, persistedHash));
        assertFalse(encoder.matches(OLD, persistedHash));
        assertEquals(1L, jdbc.queryForObject("SELECT session_version FROM user_account_security_versions WHERE user_account_id=?",
                Long.class, userId));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + oldToken)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", OLD))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", NEW))))
                .andExpect(status().isOk());
    }
}
