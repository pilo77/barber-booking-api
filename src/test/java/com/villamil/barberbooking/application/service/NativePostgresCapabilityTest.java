package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.villamil.barberbooking.domain.capabilities.CapabilityTier;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter.BillingPersistenceAdapter;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter.CapabilityJdbcAdapter;
import com.villamil.barberbooking.support.QaPostgres;

/** Persistence verification uses only the explicitly isolated QA instance or a disposable container. */
@org.junit.jupiter.api.condition.EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresCapabilityTest {
    private static final Instant NOW = Instant.parse("2026-10-08T15:00:00Z");
    private static JdbcTemplate jdbc;
    private static CapabilityJdbcAdapter repository;
    private static CapabilityService capabilities;
    private static PublicSubscriptionPolicy publicPolicy;
    private static long reviewer;

    @BeforeAll static void migrate() {
        String database = "qa_capabilities_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(QaPostgres.dataSource("postgres")).execute("CREATE DATABASE " + database);
        var data = QaPostgres.dataSource(database);
        Flyway.configure().dataSource(data).load().migrate();
        jdbc = new JdbcTemplate(data);
        repository = new CapabilityJdbcAdapter(jdbc);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        capabilities = new CapabilityService(repository, mock(CurrentUserResolver.class), clock);
        publicPolicy = new PublicSubscriptionPolicy(new BillingPersistenceAdapter(jdbc),
                repository, capabilities, clock, true);
        reviewer = jdbc.queryForObject("""
                INSERT INTO user_accounts(email, password_hash, full_name)
                VALUES (?, 'fixture-not-for-login', 'QA Reviewer') RETURNING id
                """, Long.class, "reviewer-" + UUID.randomUUID() + "@example.test");
    }

    @Test void crossTenantBranchAndPublicationCannotBeResolved() {
        Tenant first = tenant();
        Tenant second = tenant();
        publish(second);
        assertTrue(repository.findStatus(first.company(), second.branch()).isEmpty());
        assertTrue(repository.findPublicationState(first.company(), second.branch()).isEmpty());
        assertEquals("PUBLISHED", repository.findPublicationState(second.company(), second.branch()).orElseThrow());
    }

    @Test void unpaidPublishedBranchCanReceiveBookingsWithEnforcementEnabled() {
        Tenant tenant = tenant();
        publish(tenant);
        assertEquals(CapabilityTier.FREE, capabilities.forTenant(tenant.company(), tenant.branch()).tier());
        assertTrue(capabilities.forTenant(tenant.company(), tenant.branch()).basicBooking());
        assertFalse(capabilities.forTenant(tenant.company(), tenant.branch()).teamManagement());
        assertDoesNotThrow(() -> publicPolicy.requirePublicBooking(tenant.company(), tenant.branch()));
    }

    @Test void expirationPreservesBasicRightsWithoutDeletingPublication() {
        Tenant tenant = tenant();
        publish(tenant);
        jdbc.update("UPDATE company_subscriptions SET valid_until=? WHERE company_id=?",
                java.sql.Timestamp.from(NOW.plusSeconds(100)), tenant.company());
        assertTrue(capabilities.forTenant(tenant.company(), tenant.branch()).teamManagement());
        jdbc.update("UPDATE company_subscriptions SET valid_until=? WHERE company_id=?",
                java.sql.Timestamp.from(NOW), tenant.company());
        assertTrue(capabilities.forTenant(tenant.company(), tenant.branch()).basicBooking());
        assertFalse(capabilities.forTenant(tenant.company(), tenant.branch()).teamManagement());
        assertDoesNotThrow(() -> publicPolicy.requirePublicBooking(tenant.company(), tenant.branch()));
        assertEquals("PUBLISHED", repository.findPublicationState(tenant.company(), tenant.branch()).orElseThrow());
    }

    @Test void companyAndBranchSuspensionOverridePaidValidity() {
        Tenant tenant = tenant();
        publish(tenant);
        jdbc.update("UPDATE company_subscriptions SET valid_until=? WHERE company_id=?",
                java.sql.Timestamp.from(NOW.plusSeconds(100)), tenant.company());
        jdbc.update("UPDATE branches SET active=false WHERE id=?", tenant.branch());
        assertFalse(capabilities.forTenant(tenant.company(), tenant.branch()).companyActive());
        assertThrows(PublicResourceNotFoundException.class,
                () -> publicPolicy.requirePublicBooking(tenant.company(), tenant.branch()));
        jdbc.update("UPDATE branches SET active=true WHERE id=?", tenant.branch());
        jdbc.update("UPDATE companies SET active=false WHERE id=?", tenant.company());
        assertFalse(capabilities.forTenant(tenant.company(), tenant.branch()).basicBooking());
        assertFalse(capabilities.forTenant(tenant.company(), tenant.branch()).teamManagement());
    }

    @Test void draftAndLegacyBranchesDoNotGainFreePublicBookingAccidentally() {
        Tenant tenant = tenant();
        assertThrows(PublicResourceNotFoundException.class,
                () -> publicPolicy.requirePublicBooking(tenant.company(), tenant.branch()));
        jdbc.update("INSERT INTO marketplace_branch_profiles(company_id,branch_id) VALUES (?,?)",
                tenant.company(), tenant.branch());
        assertThrows(PublicResourceNotFoundException.class,
                () -> publicPolicy.requirePublicBooking(tenant.company(), tenant.branch()));
        assertEquals("DRAFT", repository.findPublicationState(tenant.company(), tenant.branch()).orElseThrow());
    }

    private Tenant tenant() {
        long company = jdbc.queryForObject("INSERT INTO companies(name,slug) VALUES ('QA Capability',?) RETURNING id",
                Long.class, "qa-cap-" + UUID.randomUUID());
        long branch = jdbc.queryForObject("INSERT INTO branches(company_id,name,slug) VALUES (?,'QA Branch','principal') RETURNING id",
                Long.class, company);
        jdbc.update("INSERT INTO company_subscriptions(company_id) VALUES (?)", company);
        return new Tenant(company, branch);
    }

    private void publish(Tenant tenant) {
        jdbc.update("""
                INSERT INTO marketplace_branch_profiles(company_id,branch_id,publication_state,
                    city,address,description,contact_phone,cover_image_url,reviewed_at,reviewed_by)
                VALUES (?,?,'PUBLISHED','QA City','QA Public Address','QA Description',
                    '0000000000','https://images.example.invalid/cover.webp',?,?)
                """, tenant.company(), tenant.branch(), java.sql.Timestamp.from(NOW), reviewer);
    }

    private record Tenant(long company, long branch) { }
}
