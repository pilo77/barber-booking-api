package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.villamil.barberbooking.application.dto.command.UpdateMarketplaceProfileCommand;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.CurrentUserProvider;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.PublicResourceNotFoundException;
import com.villamil.barberbooking.application.exception.IdempotencyConflictException;
import com.villamil.barberbooking.domain.model.*;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter.*;
import com.villamil.barberbooking.support.QaPostgres;

@EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresMarketplaceTest {
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    MarketplacePersistenceAdapter repository;
    MarketplaceService owner;
    MarketplaceService admin;
    long company, branch, ownerId, adminId;

    @BeforeEach void database() {
        String name = "qa_market_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(QaPostgres.dataSource("postgres")).execute("CREATE DATABASE " + name);
        var data = QaPostgres.dataSource(name);
        Flyway.configure().dataSource(data).load().migrate();
        jdbc = new JdbcTemplate(data); tx = new TransactionTemplate(new DataSourceTransactionManager(data));
        tx.execute(s -> new CompanyRegistrationAdapter(jdbc).create("Alpha shop", "alpha-shop", "Main", "QA Owner", "owner@example.test", "qa-only-hash"));
        company = jdbc.queryForObject("SELECT company_id FROM user_accounts WHERE email='owner@example.test'", Long.class);
        branch = jdbc.queryForObject("SELECT branch_id FROM user_accounts WHERE email='owner@example.test'", Long.class);
        ownerId = jdbc.queryForObject("SELECT id FROM user_accounts WHERE email='owner@example.test'", Long.class);
        tx.execute(s -> new PlatformOwnerProvisioningAdapter(jdbc).createIfAbsent("admin@example.test", "QA Admin", "qa-only-hash"));
        adminId = jdbc.queryForObject("SELECT id FROM user_accounts WHERE email='admin@example.test'", Long.class);
        repository = new MarketplacePersistenceAdapter(jdbc);
        owner = service(new AuthenticatedUserResponse(ownerId, "owner@example.test", "QA Owner", company, branch, null, Set.of(Role.COMPANY_OWNER)));
        admin = service(new AuthenticatedUserResponse(adminId, "admin@example.test", "QA Admin", null, null, null, Set.of(Role.PLATFORM_OWNER)));
    }

    MarketplaceService service(AuthenticatedUserResponse actor) {
        var provider = mock(CurrentUserProvider.class); when(provider.currentUser()).thenReturn(Optional.of(actor));
        return new MarketplaceService(repository, new CurrentUserResolver(provider));
    }
    void resources() {
        jdbc.update("INSERT INTO services(company_id,name,duration_minutes,price) VALUES (?, 'Corte clásico', 30, 20000)", company);
        Long barber = jdbc.queryForObject("INSERT INTO barbers(company_id,branch_id,full_name,phone) VALUES (?,?,'QA Barber','3001234567') RETURNING id", Long.class, company, branch);
        jdbc.update("INSERT INTO barber_working_hours(company_id,branch_id,barber_id,day_of_week,start_time,end_time) VALUES (?,?,?,1,'09:00','18:00')", company, branch, barber);
    }
    void draft() {
        tx.execute(s -> owner.update(new UpdateMarketplaceProfileCommand("Neiva", "Centro", "Public address", "Professional barber service", "3001234567", "https://cdn.example.test/cover.jpg")));
    }
    void publish() {
        resources(); draft(); tx.execute(s -> owner.submit());
        var version = repository.find(company, branch).orElseThrow().updatedAt();
        tx.execute(s -> { admin.review(branch, true, "Public information reviewed", version); return null; });
    }

    @Test void draftReviewPublishAndEditMaintainPublicBoundaryAndAudit() {
        assertFalse(repository.isBranchPublic(company, branch));
        assertTrue(repository.isBranchPublic(1L, 1L)); // No new profile row for preexisting legacy branch.
        assertEquals(List.of(), repository.search(null, null, null, 0, 21));
        draft();
        assertThrows(BusinessRuleException.class, () -> tx.execute(s -> owner.submit()));
        resources(); tx.execute(s -> owner.submit());
        assertEquals(1, tx.execute(s -> admin.pending()).size());
        assertFalse(repository.isBranchPublic(company, branch));
        var version = repository.find(company, branch).orElseThrow().updatedAt();
        tx.execute(s -> { admin.review(branch, true, null, version); return null; });
        assertTrue(repository.isBranchPublic(company, branch));
        var result = repository.search("neiva", "CORTE", "ALPHA", 0, 21);
        assertEquals(1, result.size()); assertEquals("alpha-shop", result.get(0).companySlug());
        assertEquals(0, repository.search(null, null, "%' OR 1=1 --", 0, 21).size());
        assertEquals(0, repository.search(null, "%", null, 0, 21).size());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM marketplace_publication_events WHERE branch_id=?", Integer.class, branch));
        draft();
        assertFalse(repository.isBranchPublic(company, branch));
        assertTrue(repository.search(null, null, null, 0, 21).isEmpty());
        var editedVersion = repository.find(company, branch).orElseThrow().updatedAt();
        assertThrows(BusinessRuleException.class, () -> tx.execute(s -> { admin.review(branch, true, null, editedVersion); return null; }));
    }

    @Test void catalogRechecksActiveBookableResourcesAndHiddenState() {
        publish();
        jdbc.update("UPDATE barber_working_hours SET active=false WHERE company_id=?", company);
        assertTrue(repository.search(null, null, null, 0, 21).isEmpty());
        jdbc.update("UPDATE barber_working_hours SET active=true WHERE company_id=?", company);
        assertEquals(1, repository.search(null, null, null, 0, 21).size());
        tx.execute(s -> owner.hide());
        assertFalse(repository.isBranchPublic(company, branch));
        assertFalse(repository.isCompanyPublic(company));
        assertTrue(repository.search(null, null, null, 0, 21).isEmpty());
    }

    @Test void tenantConstraintsAndAtomicAuditPreventCrossCompanyChanges() {
        assertThrows(DataIntegrityViolationException.class, () -> tx.execute(s -> jdbc.update(
                "INSERT INTO marketplace_branch_profiles(company_id,branch_id) VALUES (?,1)", company)));
        publish();
        var before = repository.find(company, branch).orElseThrow();
        assertThrows(DataIntegrityViolationException.class, () -> tx.execute(s -> {
            repository.lockOrCreate(company, branch);
            repository.save(before.hide(), before.publicationState(), ownerId, 1L);
            return null;
        }));
        assertEquals(MarketplacePublicationState.PUBLISHED, repository.find(company, branch).orElseThrow().publicationState());
        assertThrows(DataIntegrityViolationException.class, () -> tx.execute(s -> jdbc.update(
                "INSERT INTO marketplace_publication_events(company_id,branch_id,actor_id,from_state,to_state) VALUES (1,?,?,'DRAFT','PUBLISHED')", branch, adminId)));
    }

    @Test void concurrentApprovalPublishesOnceAndAuditsOnce() throws Exception {
        resources(); draft(); tx.execute(s -> owner.submit());
        var version = repository.find(company, branch).orElseThrow().updatedAt();
        try (var pool = Executors.newFixedThreadPool(4)) {
            Callable<Boolean> approve = () -> {
                try { tx.execute(s -> { admin.review(branch, true, null, version); return null; }); return true; }
                catch (BusinessRuleException | IdempotencyConflictException expected) { return false; }
            };
            int successes = 0;
            for (var result : pool.invokeAll(List.of(approve, approve, approve, approve), 20, TimeUnit.SECONDS)) if (result.get()) successes++;
            assertEquals(1, successes);
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM marketplace_publication_events WHERE branch_id=? AND to_state='PUBLISHED'", Integer.class, branch));
        assertEquals(MarketplacePublicationState.PUBLISHED, repository.find(company, branch).orElseThrow().publicationState());
    }

    @Test void changedResubmissionRequiresNewModeratorSnapshotWithoutAuditSideEffects() {
        resources(); draft(); tx.execute(s -> owner.submit());
        var viewed = tx.execute(s -> admin.pending()).get(0).profile();
        tx.execute(s -> owner.update(new UpdateMarketplaceProfileCommand("Neiva", "Norte", "Changed address", "Changed content",
                "3001234567", "https://cdn.example.test/changed.jpg")));
        tx.execute(s -> owner.submit());
        int events = jdbc.queryForObject("SELECT count(*) FROM marketplace_publication_events WHERE branch_id=?", Integer.class, branch);
        assertThrows(IdempotencyConflictException.class,
                () -> tx.execute(s -> { admin.review(branch, true, null, viewed.updatedAt()); return null; }));
        assertEquals(events, jdbc.queryForObject("SELECT count(*) FROM marketplace_publication_events WHERE branch_id=?", Integer.class, branch));
        assertEquals(MarketplacePublicationState.PENDING_REVIEW, repository.find(company, branch).orElseThrow().publicationState());
        assertTrue(repository.search(null, null, null, 0, 21).isEmpty());
        var fresh = tx.execute(s -> admin.pending()).get(0).profile();
        tx.execute(s -> { admin.review(branch, true, null, fresh.updatedAt()); return null; });
        assertEquals("Changed content", repository.search(null, null, null, 0, 21).get(0).description());
    }

    PublicSubscriptionPolicy bookingPolicy() {
        var status = new CapabilityJdbcAdapter(jdbc);
        var clock = java.time.Clock.systemUTC();
        var capabilities = new CapabilityService(status, mock(CurrentUserResolver.class), clock);
        return new PublicSubscriptionPolicy(new BillingPersistenceAdapter(jdbc), status, capabilities, clock, true);
    }

    @Test void bookingSharedPublicationLockDelaysHideUntilBookingTransactionCommits() throws Exception {
        publish();
        long customer = jdbc.queryForObject("INSERT INTO customers(company_id,full_name,phone) VALUES (?,'QA Customer','3009999999') RETURNING id", Long.class, company);
        long barber = jdbc.queryForObject("SELECT id FROM barbers WHERE company_id=? AND branch_id=?", Long.class, company, branch);
        long service = jdbc.queryForObject("SELECT id FROM services WHERE company_id=?", Long.class, company);
        var policy = bookingPolicy();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var hideStarted = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var booking = pool.submit(() -> tx.execute(s -> {
                policy.requirePublicBookingLocked(company, branch);
                locked.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("QA booking release timed out"); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("QA booking interrupted"); }
                return jdbc.queryForObject("""
                        INSERT INTO appointments(company_id,branch_id,customer_id,barber_id,service_offering_id,start_at,end_at,status,source)
                        VALUES (?,?,?,?,?,'2026-10-12 10:00','2026-10-12 10:30','SCHEDULED','ONLINE') RETURNING id
                        """, Long.class, company, branch, customer, barber, service);
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            var hide = pool.submit(() -> {
                hideStarted.countDown();
                return tx.execute(s -> owner.hide());
            });
            assertTrue(hideStarted.await(5, TimeUnit.SECONDS));
            try { assertThrows(TimeoutException.class, () -> hide.get(150, TimeUnit.MILLISECONDS)); }
            finally { release.countDown(); }
            long appointment = booking.get(5, TimeUnit.SECONDS);
            assertEquals(MarketplacePublicationState.HIDDEN, hide.get(5, TimeUnit.SECONDS).publicationState());
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM appointments WHERE id=? AND company_id=? AND branch_id=?", Integer.class, appointment, company, branch));
        } finally { release.countDown(); }
    }

    @Test void hideCommittingFirstRejectsNewBookingBeforeAppointmentCreation() {
        publish(); tx.execute(s -> owner.hide());
        assertThrows(PublicResourceNotFoundException.class,
                () -> tx.execute(s -> { bookingPolicy().requirePublicBookingLocked(company, branch); return null; }));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM appointments WHERE company_id=?", Integer.class, company));
    }
}
