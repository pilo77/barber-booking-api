package com.villamil.barberbooking.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.ArrayList;
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
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter.CompanyRegistrationAdapter;
import com.villamil.barberbooking.infrastructure.adapter.out.persistence.adapter.PlatformOwnerProvisioningAdapter;
import com.villamil.barberbooking.support.QaPostgres;

@EnabledIf("com.villamil.barberbooking.support.QaPostgres#available")
class NativePostgresPlatformOwnerTest {
    JdbcTemplate jdbc;
    TransactionTemplate tx;
    PlatformOwnerProvisioningService service;

    @BeforeEach void database() {
        String name = "qa_platform_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(QaPostgres.dataSource("postgres")).execute("CREATE DATABASE " + name);
        var data = QaPostgres.dataSource(name);
        Flyway.configure().dataSource(data).load().migrate();
        jdbc = new JdbcTemplate(data);
        tx = new TransactionTemplate(new DataSourceTransactionManager(data));
        var hasher = mock(PasswordHasherPort.class);
        when(hasher.hash(anyString())).thenReturn("qa-only-hash");
        service = new PlatformOwnerProvisioningService(new PlatformOwnerProvisioningAdapter(jdbc), hasher);
    }

    @Test void initialAdministratorIsGlobalAndCannotBeReplaced() {
        assertEquals(Boolean.TRUE, tx.execute(s -> service.provision("admin@example.test", "QA Administrator", "qa-only-password")));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM user_accounts u JOIN user_account_roles r ON r.user_account_id=u.id WHERE r.role='PLATFORM_OWNER' AND u.company_id IS NULL AND u.branch_id IS NULL", Integer.class));
        assertEquals(Boolean.FALSE, tx.execute(s -> service.provision("replacement@example.test", "Another Administrator", "other-password")));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user_accounts WHERE email='replacement@example.test'", Integer.class));
        assertEquals("qa-only-hash", jdbc.queryForObject("SELECT password_hash FROM user_accounts WHERE email='admin@example.test'", String.class));
    }

    @Test void concurrentDeploymentsCreateOneAdministratorOnly() throws Exception {
        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<Callable<Boolean>>();
            for (int i=0; i<4; i++) tasks.add(() -> tx.execute(s -> service.provision("admin@example.test", "QA Administrator", "qa-only-password")));
            int created = 0;
            for (var result : pool.invokeAll(tasks, 20, TimeUnit.SECONDS)) if (result.get()) created++;
            assertEquals(1, created);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM user_account_roles WHERE role='PLATFORM_OWNER'", Integer.class));
        }
    }

    @Test void existingCompanyOwnerIsNeverPromotedOrPasswordReset() {
        var registration = new CompanyRegistrationAdapter(jdbc);
        tx.execute(s -> registration.create("QA Shop", "qa-owner-conflict", "Main", "QA Owner", "owner@example.test", "original-qa-hash"));
        assertThrows(IllegalStateException.class,
                () -> tx.execute(s -> service.provision("owner@example.test", "QA Administrator", "qa-only-password")));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user_account_roles WHERE role='PLATFORM_OWNER'", Integer.class));
        assertEquals("original-qa-hash", jdbc.queryForObject("SELECT password_hash FROM user_accounts WHERE email='owner@example.test'", String.class));
    }
}
