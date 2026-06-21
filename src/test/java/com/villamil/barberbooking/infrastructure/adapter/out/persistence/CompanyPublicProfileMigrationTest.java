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
class CompanyPublicProfileMigrationTest {

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("barber_booking_hu24")
			.withUsername("barber_test")
			.withPassword("barber_test");

	@BeforeAll
	static void migrateFromV13ToLatest() {
		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.target("13")
				.load()
				.migrate();
		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.load()
				.migrate();
	}

	@Test
	void tableBackfillAndUniqueConstraintExist() throws Exception {
		try (var connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		); var statement = connection.createStatement()) {
			try (var rows = statement.executeQuery("""
					SELECT public_name, public_description, logo_url, theme_mode
					FROM company_public_profiles
					WHERE company_id = 1
					""")) {
				assertThat(rows.next()).isTrue();
				assertThat(rows.getString("public_name")).isEqualTo("Default Barber Company");
				assertThat(rows.getString("theme_mode")).isEqualTo("SYSTEM");
			}

			assertThatThrownBy(() -> statement.executeUpdate("""
						INSERT INTO company_public_profiles (company_id, theme_mode, created_at, updated_at)
						VALUES (1, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
						"""))
					.isInstanceOf(SQLException.class)
					.satisfies(exception -> assertThat(((SQLException) exception).getSQLState()).isEqualTo("23505"));
		}
	}

	@Test
	void foreignKeyAndChecksAreEnforced() throws Exception {
		try (var connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		); var statement = connection.createStatement()) {
			assertThatThrownBy(() -> statement.executeUpdate("""
						INSERT INTO company_public_profiles (company_id, theme_mode, created_at, updated_at)
						VALUES (999, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
						"""))
					.isInstanceOf(SQLException.class)
					.satisfies(exception -> assertThat(((SQLException) exception).getSQLState()).isEqualTo("23503"));
		}
	}

	@Test
	void themeModeAndHexColorChecksAreEnforced() throws Exception {
		try (var connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		); var statement = connection.createStatement()) {
			statement.executeUpdate("""
					INSERT INTO companies (id, name, slug) VALUES (24, 'HU24 Company', 'hu24-company')
					""");

			assertThatThrownBy(() -> statement.executeUpdate("""
						INSERT INTO company_public_profiles (
							company_id, theme_mode, created_at, updated_at
						) VALUES (24, 'BLUE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
						"""))
					.isInstanceOf(SQLException.class)
					.satisfies(exception -> assertThat(((SQLException) exception).getSQLState()).isEqualTo("23514"));

			assertThatThrownBy(() -> statement.executeUpdate("""
						INSERT INTO company_public_profiles (
							company_id, theme_mode, primary_color, created_at, updated_at
						) VALUES (24, 'SYSTEM', '111111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
						"""))
					.isInstanceOf(SQLException.class)
					.satisfies(exception -> assertThat(((SQLException) exception).getSQLState()).isEqualTo("23514"));
		}
	}
}
