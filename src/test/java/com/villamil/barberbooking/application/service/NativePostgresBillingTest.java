package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.villamil.barberbooking.application.billing.VerifiedPayment;
import com.villamil.barberbooking.application.port.out.PaymentCheckoutPort;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter.BillingPersistenceAdapter;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter.CompanyRegistrationAdapter;

/** Runs only against the explicitly isolated local QA instance; never a cloud/customer database. */
@org.junit.jupiter.api.condition.EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresBillingTest {
    static JdbcTemplate jdbc; static TransactionTemplate tx; static BillingPersistenceAdapter billing;
    static CompanyRegistrationAdapter registration;
    @BeforeAll static void migrate() {
        String database = "qa_billing_" + UUID.randomUUID().toString().replace("-", "");
        var base = com.villamil.barberbooking.support.QaPostgres.dataSource("postgres");
        var admin = new JdbcTemplate(base);
        admin.execute("CREATE DATABASE " + database);
        var data = com.villamil.barberbooking.support.QaPostgres.dataSource(database);
        jdbc = new JdbcTemplate(data); tx = new TransactionTemplate(new DataSourceTransactionManager(data));
        Flyway.configure().dataSource(data).target("14").load().migrate();
        assertEquals(14, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success AND type = 'SQL'", Integer.class));
        Flyway.configure().dataSource(data).load().migrate();
        assertEquals(19, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success AND type = 'SQL'", Integer.class));
        assertTrue(Flyway.configure().dataSource(data).load().validateWithResult().validationSuccessful);
        billing = new BillingPersistenceAdapter(jdbc); registration = new CompanyRegistrationAdapter(jdbc);
    }
    long company() {
        String slug = "test-" + UUID.randomUUID().toString();
        tx.execute(status -> registration.create("QA Barber", slug, "Principal", "QA Owner", slug + "@example.test", "not-a-real-login-hash"));
        return jdbc.queryForObject("SELECT id FROM companies WHERE slug = ?", Long.class, slug);
    }
    @Test void onboardingAndDuplicateEmailRollBackAtomically() {
        long company = company(); assertFalse(billing.subscription(company).activeAt(Instant.now()));
        String email = jdbc.queryForObject("SELECT email FROM user_accounts WHERE company_id = ?", String.class, company);
        String duplicateSlug = "duplicate-" + UUID.randomUUID();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
          () -> tx.execute(status -> registration.create("Duplicate", duplicateSlug, "Principal", "Owner", email, "fixture-hash")));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM companies WHERE slug = ?", Integer.class, duplicateSlug));
    }
    @Test void simultaneousCheckoutRetriesCreateExactlyOneTenantScopedOrder() throws Exception {
        long company = company(); String key = UUID.randomUUID().toString();
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<String>>();
            for (int i = 0; i < 8; i++) tasks.add(() -> tx.execute(status -> billing.createOrGetOrder(company, key, 500000, "test").reference()));
            var refs = new java.util.HashSet<String>();
            for (var future : pool.invokeAll(tasks, 20, TimeUnit.SECONDS)) refs.add(future.get());
            assertEquals(1, refs.size());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM subscription_payment_orders WHERE company_id = ?", Integer.class, company));
            long other = company(); assertNotEquals(refs.iterator().next(), tx.execute(status -> billing.createOrGetOrder(other, key, 500000, "test").reference()));
            assertTrue(billing.findOrder(refs.iterator().next(), other).isEmpty());
        }
    }
    @Test void concurrentDuplicateWebhooksExtendSubscriptionOnlyOnce() throws Exception {
        long company = company(); var order = tx.execute(status -> billing.createOrGetOrder(company, UUID.randomUUID().toString(), 500000, "test"));
        PaymentCheckoutPort gateway = mock(PaymentCheckoutPort.class); when(gateway.enabled()).thenReturn(true); when(gateway.environment()).thenReturn("test");
        var service = new BillingService(billing, gateway, mock(CurrentUserResolver.class), 500000);
        var payment = new VerifiedPayment(order.reference(), "qa-" + UUID.randomUUID(), 500000, "COP", "test", "APPROVED");
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<Void>>();
            for (int i = 0; i < 8; i++) tasks.add(() -> tx.execute(status -> { service.confirm(payment); return null; }));
            for (var future : pool.invokeAll(tasks, 20, TimeUnit.SECONDS)) future.get();
        }
        assertTrue(billing.findOrder(order.reference(), company).orElseThrow().paid());
        Instant until = billing.subscription(company).validUntil();
        assertTrue(until.isAfter(Instant.now().plusSeconds(27 * 86400)));
        assertTrue(until.isBefore(Instant.now().plusSeconds(32 * 86400)));
    }
    @Test void aProviderTransactionCannotPayTwoOrders() {
        long company = company(); var first = tx.execute(status -> billing.createOrGetOrder(company, UUID.randomUUID().toString(), 500000, "test"));
        var second = tx.execute(status -> billing.createOrGetOrder(company, UUID.randomUUID().toString(), 500000, "test"));
        String transaction = "qa-" + UUID.randomUUID();
        tx.execute(status -> { billing.markPaid(first.reference(), transaction, Instant.now()); return null; });
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
          () -> tx.execute(status -> { billing.markPaid(second.reference(), transaction, Instant.now()); return null; }));
        assertFalse(billing.findOrder(second.reference(), company).orElseThrow().paid());
    }
}
