package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.villamil.barberbooking.domain.exception.AuthenticationFailedException;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter.PasswordSecurityJdbcAdapter;
import com.villamil.barberbooking.infrastructure.security.JwtTokenAdapter;
import com.villamil.barberbooking.support.QaPostgres;

@org.junit.jupiter.api.condition.EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresPasswordSecurityTest {
    private static JdbcTemplate jdbc;
    private static PasswordSecurityJdbcAdapter security;
    private static TransactionTemplate tx;

    @BeforeAll static void migrate() {
        String database = "qa_password_cas_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(QaPostgres.dataSource("postgres")).execute("CREATE DATABASE " + database);
        var data = QaPostgres.dataSource(database);
        Flyway.configure().dataSource(data).load().migrate();
        jdbc = new JdbcTemplate(data);
        security = new PasswordSecurityJdbcAdapter(jdbc);
        tx = new TransactionTemplate(new DataSourceTransactionManager(data));
    }

    @Test void concurrentChangesUsingSameOldHashHaveExactlyOneWinnerAndOneVersionIncrement() throws Exception {
        long user = account("fixture-original-hash");
        try (var pool = Executors.newFixedThreadPool(8)) {
            var calls = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 8; i++) {
                String replacement = "fixture-replacement-" + i;
                calls.add(() -> tx.execute(status -> security.compareAndChangePassword(user, "fixture-original-hash", replacement)));
            }
            int success = 0;
            for (var result : pool.invokeAll(calls, 20, TimeUnit.SECONDS)) if (result.get()) success++;
            assertEquals(1, success);
        }
        assertEquals(1L, security.currentVersion(user));
        assertTrue(security.currentVersionForPasswordSnapshot(user, "fixture-original-hash").isEmpty());
    }

    @Test void rollbackRestoresHashAndSessionVersionTogether() {
        long user = account("fixture-original-hash");
        tx.execute(status -> {
            assertTrue(security.compareAndChangePassword(user, "fixture-original-hash", "fixture-new-hash"));
            status.setRollbackOnly();
            return null;
        });
        assertEquals(0L, security.currentVersion(user));
        assertTrue(security.currentVersionForPasswordSnapshot(user, "fixture-original-hash").isPresent());
        assertTrue(security.currentVersionForPasswordSnapshot(user, "fixture-new-hash").isEmpty());
    }

    @Test void staleLoginSnapshotCannotProduceTokenAfterSuccessfulChange() {
        long user = account("fixture-original-hash");
        var snapshot = new UserAccount(user, null, null, "snapshot@example.test", "fixture-original-hash", "QA User",
                null, null, true, Instant.EPOCH, Instant.EPOCH, Set.of(Role.PLATFORM_OWNER));
        var tokens = new JwtTokenAdapter(UUID.randomUUID().toString() + UUID.randomUUID(), 15,
                "qa-issuer", "qa-audience", security);
        String issuedBefore = tokens.createAccessToken(snapshot);
        tx.execute(status -> security.compareAndChangePassword(user, "fixture-original-hash", "fixture-new-hash"));
        assertThrows(IllegalArgumentException.class, () -> tokens.parse(issuedBefore));
        assertThrows(AuthenticationFailedException.class, () -> tokens.createAccessToken(snapshot));
    }

    @Test void inactiveAccountNeverChangesHashOrAcquiresSecurityVersion() {
        long user = account("fixture-original-hash");
        jdbc.update("UPDATE user_accounts SET active=false WHERE id=?", user);
        Boolean changed = tx.execute(status -> security.compareAndChangePassword(user, "fixture-original-hash", "fixture-new-hash"));
        assertFalse(changed);
        assertEquals(0L, security.currentVersion(user));
        assertTrue(security.currentVersionForPasswordSnapshot(user, "fixture-original-hash").isEmpty());
    }

    private long account(String hash) {
        return jdbc.queryForObject("""
                INSERT INTO user_accounts(email,password_hash,full_name)
                VALUES (?,?,'QA Security') RETURNING id
                """, Long.class, "qa-cas-" + UUID.randomUUID() + "@example.test", hash);
    }
}
