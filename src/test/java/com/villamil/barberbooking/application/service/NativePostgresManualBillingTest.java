package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.exception.IdempotencyConflictException;
import com.villamil.barberbooking.application.port.out.CurrentUserProvider;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter.*;
import com.villamil.barberbooking.support.QaPostgres;

@EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresManualBillingTest {
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    BillingPersistenceAdapter billing;
    ManualPaymentPersistenceAdapter reports;
    ManualBillingService owner;
    ManualBillingService admin;
    long company;
    long ownerId;

    @BeforeEach void database() {
        String name = "qa_manual_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(QaPostgres.dataSource("postgres")).execute("CREATE DATABASE " + name);
        var data = QaPostgres.dataSource(name);
        Flyway.configure().dataSource(data).load().migrate();
        jdbc = new JdbcTemplate(data); tx = new TransactionTemplate(new DataSourceTransactionManager(data));
        tx.execute(s -> new CompanyRegistrationAdapter(jdbc).create("QA Shop", "qa-manual", "Main", "QA Owner", "owner@example.test", "qa-only-hash"));
        company = jdbc.queryForObject("SELECT company_id FROM user_accounts WHERE email='owner@example.test'", Long.class);
        long branch = jdbc.queryForObject("SELECT branch_id FROM user_accounts WHERE email='owner@example.test'", Long.class);
        ownerId = jdbc.queryForObject("SELECT id FROM user_accounts WHERE email='owner@example.test'", Long.class);
        tx.execute(s -> new PlatformOwnerProvisioningAdapter(jdbc).createIfAbsent("admin@example.test", "QA Admin", "qa-only-hash"));
        long adminId = jdbc.queryForObject("SELECT id FROM user_accounts WHERE email='admin@example.test'", Long.class);
        billing = new BillingPersistenceAdapter(jdbc); reports = new ManualPaymentPersistenceAdapter(jdbc);
        owner = service(new AuthenticatedUserResponse(ownerId, "owner@example.test", "QA Owner", company, branch, null, Set.of(Role.COMPANY_OWNER)));
        admin = service(new AuthenticatedUserResponse(adminId, "admin@example.test", "QA Admin", null, null, null, Set.of(Role.PLATFORM_OWNER)));
    }

    ManualBillingService service(AuthenticatedUserResponse user) {
        var provider = mock(CurrentUserProvider.class); when(provider.currentUser()).thenReturn(Optional.of(user));
        return new ManualBillingService(reports, billing, new CurrentUserResolver(provider), 4000000, "QA transfer destination");
    }

    @Test void concurrentApprovalsApplyOneMonthAndOneAuditRecord() throws Exception {
        var report = tx.execute(s -> owner.report(UUID.randomUUID().toString(), "qa-transfer-123"));
        assertEquals("owner@example.test", report.reporterEmail());
        assertFalse(billing.subscription(company).activeAt(Instant.now()));
        try (var pool = Executors.newFixedThreadPool(4)) {
            Callable<Void> approve = () -> tx.execute(s -> { admin.approve(report.reference(), "qa-bank-123", 4000000); return null; });
            for (var result : pool.invokeAll(List.of(approve, approve, approve, approve), 20, TimeUnit.SECONDS)) result.get();
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM subscription_payment_reviews WHERE order_reference=?::uuid", Integer.class, report.reference()));
        Instant until = billing.subscription(company).validUntil();
        assertTrue(until.isAfter(Instant.now().plusSeconds(27*86400)));
        assertTrue(until.isBefore(Instant.now().plusSeconds(32*86400)));
    }

    @Test void bankTransactionCannotApproveAnotherReportAndRejectionDoesNotActivate() {
        var first = tx.execute(s -> owner.report(UUID.randomUUID().toString(), "qa-transfer-111"));
        var second = tx.execute(s -> owner.report(UUID.randomUUID().toString(), "qa-transfer-222"));
        tx.execute(s -> { admin.approve(first.reference(), "qa-bank-unique", 4000000); return null; });
        Instant until = billing.subscription(company).validUntil();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> tx.execute(s -> { admin.approve(second.reference(), "qa-bank-unique", 4000000); return null; }));
        assertEquals("PENDING", tx.execute(s -> reports.lockReport(second.reference()).orElseThrow().status()));
        assertEquals(until, billing.subscription(company).validUntil());
        tx.execute(s -> { admin.reject(second.reference(), "No matching bank receipt"); return null; });
        assertThrows(IdempotencyConflictException.class,
                () -> tx.execute(s -> { admin.approve(second.reference(), "qa-bank-other", 4000000); return null; }));
        assertEquals(until, billing.subscription(company).validUntil());
    }

    @Test void reporterForeignKeyRejectsAnotherTenantAndCrossMethodKeyConflicts() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> tx.execute(s -> reports.createReport(1L, ownerId, UUID.randomUUID().toString(), 4000000, "qa-transfer-333")));
        String key = UUID.randomUUID().toString();
        tx.execute(s -> billing.createOrGetOrder(company, key, 4000000, "test"));
        assertThrows(IdempotencyConflictException.class,
                () -> tx.execute(s -> owner.report(key, "qa-transfer-444")));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM subscription_payment_orders WHERE payment_method='MANUAL'", Integer.class));
    }
}
