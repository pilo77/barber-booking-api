package com.villamil.barberbooking.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PublicBookingIdempotencyMigrationTest {

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("barber_booking_hu22")
			.withUsername("barber_test")
			.withPassword("barber_test");

	@BeforeAll
	static void migrateFromV11ToLatest() {
		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.target("11")
				.load()
				.migrate();
		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.load()
				.migrate();
	}

	@Test
	void uniqueKeyIsEnforcedWithinTenantAndReusableAcrossBranches() throws Exception {
		try (var connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		); var statement = connection.createStatement()) {
			statement.executeUpdate("""
					INSERT INTO branches (company_id, name, slug)
					VALUES (1, 'Second Branch', 'second-branch')
					""");
			statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, status
					) VALUES (1, 1, 'shared-key-123', repeat('a', 64), 'IN_PROGRESS')
					""");

			assertThatThrownBy(() -> statement.executeUpdate("""
						INSERT INTO public_booking_idempotency_keys (
							company_id, branch_id, idempotency_key, request_hash, status
						) VALUES (1, 1, 'shared-key-123', repeat('a', 64), 'IN_PROGRESS')
						"""))
					.isInstanceOf(SQLException.class)
					.satisfies(exception -> assertThat(((SQLException) exception).getSQLState()).isEqualTo("23505"));

			int inserted = statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, status
					) VALUES (1, 2, 'shared-key-123', repeat('a', 64), 'IN_PROGRESS')
					""");
			assertThat(inserted).isEqualTo(1);
		}
	}
}
