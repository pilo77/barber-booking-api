package com.villamil.barberbooking.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
						company_id, branch_id, idempotency_key, request_hash, claim_token, status
					) VALUES (1, 1, 'shared-key-123', repeat('a', 64), 'claim-a', 'IN_PROGRESS')
					""");

			assertThatThrownBy(() -> statement.executeUpdate("""
						INSERT INTO public_booking_idempotency_keys (
							company_id, branch_id, idempotency_key, request_hash, claim_token, status
						) VALUES (1, 1, 'shared-key-123', repeat('a', 64), 'claim-b', 'IN_PROGRESS')
						"""))
					.isInstanceOf(SQLException.class)
					.satisfies(exception -> assertThat(((SQLException) exception).getSQLState()).isEqualTo("23505"));

			int inserted = statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status
					) VALUES (1, 2, 'shared-key-123', repeat('a', 64), 'claim-c', 'IN_PROGRESS')
					""");
			assertThat(inserted).isEqualTo(1);
		}
	}

	@Test
	void rolledBackInProgressClaimDoesNotBlockRetry() throws Exception {
		try (var connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		); var statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status
					) VALUES (1, 1, 'rollback-key-123', repeat('b', 64), 'claim-rollback-a', 'IN_PROGRESS')
					""");
			connection.rollback();
			connection.setAutoCommit(true);

			int inserted = statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status
					) VALUES (1, 1, 'rollback-key-123', repeat('b', 64), 'claim-rollback-b', 'IN_PROGRESS')
					""");
			assertThat(inserted).isEqualTo(1);
		}
	}

	@Test
	void idempotencyKeyCannotReferenceAppointmentFromAnotherTenant() throws Exception {
		try (var connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		); var statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO companies (id, name, slug) VALUES (20, 'Other Company', 'other-company')");
			statement.executeUpdate("""
					INSERT INTO branches (id, company_id, name, slug)
					VALUES (20, 20, 'Other Branch', 'other-branch')
					""");
			statement.executeUpdate("""
					INSERT INTO customers (id, company_id, full_name, phone, active, updated_at)
					VALUES (20, 20, 'Other Customer', '3990000020', true, now())
					""");
			statement.executeUpdate("""
					INSERT INTO barbers (
						id, company_id, branch_id, full_name, phone, active, updated_at
					) VALUES (20, 20, 20, 'Other Barber', '3980000020', true, now())
					""");
			statement.executeUpdate("""
					INSERT INTO services (
						id, company_id, name, duration_minutes, price, active, updated_at
					) VALUES (20, 20, 'Other Service', 30, 25000, true, now())
					""");
			statement.executeUpdate("""
					INSERT INTO appointments (
						id, company_id, branch_id, customer_id, barber_id, service_offering_id,
						start_at, end_at, status, source
					) VALUES (
						20, 20, 20, 20, 20, 20,
						'2026-06-22 10:00:00', '2026-06-22 10:30:00', 'SCHEDULED', 'ONLINE'
					)
					""");

			assertThatThrownBy(() -> statement.executeUpdate("""
						INSERT INTO public_booking_idempotency_keys (
							company_id, branch_id, idempotency_key, request_hash,
							claim_token, appointment_id, status, completed_at
						) VALUES (
							1, 1, 'cross-tenant-key', repeat('c', 64), 'claim-cross-tenant',
							20, 'COMPLETED', now()
						)
						"""))
					.isInstanceOf(SQLException.class)
					.satisfies(exception -> assertThat(((SQLException) exception).getSQLState()).isEqualTo("23503"));
		}
	}

	@Test
	void concurrentClaimsCreateOnlyOneIdempotencyRow() throws Exception {
		try (var firstConnection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		); var firstStatement = firstConnection.createStatement()) {
			firstConnection.setAutoCommit(false);
			firstStatement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status
					) VALUES (1, 1, 'concurrent-key-123', repeat('d', 64), 'claim-concurrent-a', 'IN_PROGRESS')
					ON CONFLICT (company_id, branch_id, idempotency_key) DO NOTHING
					""");

			CompletableFuture<Integer> competingClaim = CompletableFuture.supplyAsync(() -> {
				try (var connection = DriverManager.getConnection(
						POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
				); var statement = connection.createStatement()) {
					return statement.executeUpdate("""
							INSERT INTO public_booking_idempotency_keys (
								company_id, branch_id, idempotency_key, request_hash, claim_token, status
							) VALUES (1, 1, 'concurrent-key-123', repeat('d', 64), 'claim-concurrent-b', 'IN_PROGRESS')
							ON CONFLICT (company_id, branch_id, idempotency_key) DO NOTHING
							""");
				} catch (SQLException exception) {
					throw new IllegalStateException(exception);
				}
			});

			assertThatThrownBy(() -> competingClaim.get(250, TimeUnit.MILLISECONDS))
					.isInstanceOf(TimeoutException.class);
			firstConnection.commit();

			assertThat(competingClaim.get(5, TimeUnit.SECONDS)).isZero();
			try (var verification = DriverManager.getConnection(
					POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
			); var statement = verification.createStatement(); var rows = statement.executeQuery("""
					SELECT count(*)
					FROM public_booking_idempotency_keys
					WHERE company_id = 1
						AND branch_id = 1
						AND idempotency_key = 'concurrent-key-123'
					""")) {
				rows.next();
				assertThat(rows.getInt(1)).isEqualTo(1);
			}
		}
	}

	@Test
	void staleInProgressClaimCanBeReclaimedButFreshOneCannot() throws Exception {
		try (var connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		); var statement = connection.createStatement()) {
			statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status, created_at
					) VALUES (
						1, 1, 'stale-key-123', repeat('e', 64), 'claim-stale-a', 'IN_PROGRESS',
						CURRENT_TIMESTAMP - INTERVAL '3 minutes'
					)
					""");
			int reclaimed = statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status
					) VALUES (
						1, 1, 'stale-key-123', repeat('f', 64), 'claim-stale-b', 'IN_PROGRESS'
					)
					ON CONFLICT (company_id, branch_id, idempotency_key) DO UPDATE
					SET request_hash = EXCLUDED.request_hash,
						claim_token = EXCLUDED.claim_token,
						status = 'IN_PROGRESS',
						appointment_id = NULL,
						completed_at = NULL,
						created_at = CURRENT_TIMESTAMP
					WHERE public_booking_idempotency_keys.status = 'IN_PROGRESS'
						AND public_booking_idempotency_keys.created_at <= (
							CURRENT_TIMESTAMP - make_interval(secs => 120)
						)
					""");
			assertThat(reclaimed).isEqualTo(1);

			statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status
					) VALUES (1, 1, 'fresh-key-123', repeat('g', 64), 'claim-fresh-a', 'IN_PROGRESS')
					""");
			int blocked = statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status
					) VALUES (
						1, 1, 'fresh-key-123', repeat('h', 64), 'claim-fresh-b', 'IN_PROGRESS'
					)
					ON CONFLICT (company_id, branch_id, idempotency_key) DO UPDATE
					SET request_hash = EXCLUDED.request_hash,
						claim_token = EXCLUDED.claim_token,
						status = 'IN_PROGRESS',
						appointment_id = NULL,
						completed_at = NULL,
						created_at = CURRENT_TIMESTAMP
					WHERE public_booking_idempotency_keys.status = 'IN_PROGRESS'
						AND public_booking_idempotency_keys.created_at <= (
							CURRENT_TIMESTAMP - make_interval(secs => 120)
						)
					""");
			assertThat(blocked).isZero();
		}
	}

	@Test
	void staleCompletionCannotHijackReclaimedClaim() throws Exception {
		try (var connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		); var statement = connection.createStatement()) {
			statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status, created_at
					) VALUES (
						1, 1, 'fenced-key-123', repeat('i', 64), 'claim-old', 'IN_PROGRESS',
						CURRENT_TIMESTAMP - INTERVAL '3 minutes'
					)
					""");
			statement.executeUpdate("""
					INSERT INTO public_booking_idempotency_keys (
						company_id, branch_id, idempotency_key, request_hash, claim_token, status
					) VALUES (
						1, 1, 'fenced-key-123', repeat('i', 64), 'claim-new', 'IN_PROGRESS'
					)
					ON CONFLICT (company_id, branch_id, idempotency_key) DO UPDATE
					SET request_hash = EXCLUDED.request_hash,
						claim_token = EXCLUDED.claim_token,
						status = 'IN_PROGRESS',
						appointment_id = NULL,
						completed_at = NULL,
						created_at = CURRENT_TIMESTAMP
					WHERE public_booking_idempotency_keys.status = 'IN_PROGRESS'
						AND public_booking_idempotency_keys.created_at <= (
							CURRENT_TIMESTAMP - make_interval(secs => 120)
						)
					""");

			int staleCompletion = statement.executeUpdate("""
					UPDATE public_booking_idempotency_keys
					SET status = 'COMPLETED',
						appointment_id = 1,
						completed_at = CURRENT_TIMESTAMP
					WHERE company_id = 1
						AND branch_id = 1
						AND idempotency_key = 'fenced-key-123'
						AND claim_token = 'claim-old'
						AND status = 'IN_PROGRESS'
					""");
			assertThat(staleCompletion).isZero();
		}
	}
}
