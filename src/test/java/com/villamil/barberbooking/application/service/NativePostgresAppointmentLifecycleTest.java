package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.in.CancelAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.StartAppointmentUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.TenantContextExecutor;
import com.villamil.barberbooking.application.tenant.TenantContext;
import com.villamil.barberbooking.domain.exception.AppointmentInvalidStatusTransitionException;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;
import com.villamil.barberbooking.infrastructure.security.AuthenticatedUserPrincipal;
import com.villamil.barberbooking.support.QaPostgres;

@SpringBootTest(properties={"spring.config.import=", "app.bootstrap.token=", "billing.enforce-subscription=true"})
@EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresAppointmentLifecycleTest {

    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        String database = "qa_lifecycle_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(QaPostgres.dataSource("postgres")).execute("CREATE DATABASE " + database);
        var data = QaPostgres.dataSource(database);
        properties.add("spring.datasource.url", data::getUrl);
        properties.add("spring.datasource.username", data::getUsername);
        properties.add("spring.datasource.password", data::getPassword);
        properties.add("app.jwt.secret", () -> UUID.randomUUID().toString() + UUID.randomUUID());
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired AppointmentRepositoryPort appointments;
    @Autowired CancelAppointmentUseCase cancel;
    @Autowired StartAppointmentUseCase start;
    @Autowired TenantContextExecutor tenants;
    @Autowired PlatformTransactionManager transactions;

    long company, branch, otherBranch, barber, appointment;

    @BeforeEach void seed() {
        String suffix = UUID.randomUUID().toString();
        company = jdbc.queryForObject("INSERT INTO companies(name,slug) VALUES ('Lifecycle QA',?) RETURNING id", Long.class, "qa-" + suffix);
        branch = jdbc.queryForObject("INSERT INTO branches(company_id,name,slug) VALUES (?,'Main','main') RETURNING id", Long.class, company);
        otherBranch = jdbc.queryForObject("INSERT INTO branches(company_id,name,slug) VALUES (?,'Other','other') RETURNING id", Long.class, company);
        barber = jdbc.queryForObject("INSERT INTO barbers(company_id,branch_id,full_name,phone) VALUES (?,?,'QA Barber',?) RETURNING id", Long.class, company, branch, suffix.substring(0, 20));
        long customer = jdbc.queryForObject("INSERT INTO customers(company_id,full_name,phone) VALUES (?,'QA Customer',?) RETURNING id", Long.class, company, suffix.substring(0, 20));
        long service = jdbc.queryForObject("INSERT INTO services(company_id,name,duration_minutes,price) VALUES (?,'QA Service',30,20000) RETURNING id", Long.class, company);
        appointment = jdbc.queryForObject("""
                INSERT INTO appointments(company_id,branch_id,customer_id,barber_id,service_offering_id,start_at,end_at,status,source)
                VALUES (?,?,?,?,?,'2026-10-20 09:00','2026-10-20 09:30','SCHEDULED','ONLINE') RETURNING id
                """, Long.class, company, branch, customer, barber, service);
    }

    @Test void concurrentStartWaitsAndCannotOverwriteCommittedCancellation() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var competingStarted = new CountDownLatch(1);
        var owner = actor(Role.COMPANY_OWNER, company, branch, null);
        var scope = new TenantContext(company, branch);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var cancellation = workers.submit(() -> as(owner, scope, () ->
                new TransactionTemplate(transactions).execute(transaction -> {
                    assertTrue(appointments.findByIdForUpdate(appointment).isPresent());
                    locked.countDown();
                    await(release);
                    return cancel.cancel(appointment).status();
                })));
            assertTrue(locked.await(10, TimeUnit.SECONDS), "First transaction must acquire its row lock");
            var competingStart = workers.submit(() -> as(owner, scope, () -> {
                competingStarted.countDown();
                return start.start(appointment).status();
            }));
            try {
                assertTrue(competingStarted.await(10, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> competingStart.get(300, TimeUnit.MILLISECONDS),
                        "A competing lifecycle operation must wait for the transaction holding the row lock");
            } finally {
                release.countDown();
            }
            assertEquals(AppointmentStatus.CANCELLED, cancellation.get(10, TimeUnit.SECONDS));
            var conflict = assertThrows(ExecutionException.class, () -> competingStart.get(10, TimeUnit.SECONDS));
            assertInstanceOf(AppointmentInvalidStatusTransitionException.class, conflict.getCause());
        } finally {
            release.countDown();
        }
        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM appointments WHERE id=?", String.class, appointment));
    }

    @Test void lockingDoesNotBroadenBarberOrBranchManagerScope() {
        var scope = new TenantContext(company, branch);
        assertThrows(ForbiddenOperationException.class, () ->
                as(actor(Role.BARBER, company, branch, barber + 1000), scope, () -> start.start(appointment)));
        assertThrows(AppointmentNotFoundException.class, () ->
                as(actor(Role.BRANCH_MANAGER, company, otherBranch, null), new TenantContext(company, otherBranch),
                        () -> start.start(appointment)));
        assertThrows(AppointmentNotFoundException.class, () ->
                as(actor(Role.COMPANY_OWNER, company + 1000, branch, null), new TenantContext(company + 1000, branch),
                        () -> start.start(appointment)));
        assertEquals("SCHEDULED", jdbc.queryForObject("SELECT status FROM appointments WHERE id=?", String.class, appointment));
    }

    private AuthenticatedUserResponse actor(Role role, long companyId, long branchId, Long barberId) {
        return new AuthenticatedUserResponse(1000L, "qa@example.test", "Lifecycle QA", companyId, branchId, barberId, Set.of(role));
    }

    private <T> T as(AuthenticatedUserResponse user, TenantContext scope, Supplier<T> action) {
        var context = SecurityContextHolder.createEmptyContext();
        var principal = new AuthenticatedUserPrincipal(user, List.of());
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        SecurityContextHolder.setContext(context);
        try {
            return tenants.withTenant(scope, action);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Concurrent transaction did not release its lock");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
