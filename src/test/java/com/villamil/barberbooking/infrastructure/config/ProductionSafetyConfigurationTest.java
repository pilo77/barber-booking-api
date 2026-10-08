package com.villamil.barberbooking.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class ProductionSafetyConfigurationTest {
    private static final String DATABASE = "jdbc:postgresql://database.example.invalid/barber_booking?sslmode=verify-full";
    private static final String SSL_FACTORY = "org.postgresql.ssl.DefaultJavaSSLFactory";
    private static final String ORIGINS = "https://frontend.example.invalid";

    @Test
    void acceptsVerifiedRemoteDatabaseAndExplicitHttpsOrigins() {
        assertThatCode(() -> safety(DATABASE, SSL_FACTORY, ORIGINS)).doesNotThrowAnyException();
        assertThatCode(() -> safety(DATABASE + "&sslfactory=" + SSL_FACTORY, SSL_FACTORY,
                ORIGINS + ",https://second.example.invalid")).doesNotThrowAnyException();
    }

    @Test
    void productionProfileUsesJvmCertificateTrustStore() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));
        assertThat(yaml.getObject()).containsEntry("spring.datasource.hikari.data-source-properties.sslfactory", SSL_FACTORY);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:postgresql://database.example.invalid/barber_booking",
        "jdbc:postgresql://database.example.invalid/barber_booking?sslmode=require",
        "jdbc:postgresql://database.example.invalid/barber_booking?sslmode=verify-full&sslmode=require",
        "jdbc:postgresql://database.example.invalid/barber_booking?sslmode=verify-full&%73slmode=require",
        "jdbc:postgresql://database.example.invalid/barber_booking?sslmode=verify-full&sslfactory=org.postgresql.ssl.NonValidatingFactory",
        "jdbc:postgresql://database.example.invalid/barber_booking?sslmode=verify-full&%73slfactory=org.postgresql.ssl.NonValidatingFactory",
        "jdbc:postgresql://database.example.invalid/barber_booking?sslmode=verify-full&sslhostnameverifier=custom.Verifier",
        "jdbc:postgresql://fixture-user:fixture-only-password@database.example.invalid/barber_booking?sslmode=verify-full",
        "jdbc:postgresql://localhost/barber_booking?sslmode=verify-full",
        "jdbc:postgresql://LOCALHOST./barber_booking?sslmode=verify-full",
        "jdbc:postgresql://127.0.0.2/barber_booking?sslmode=verify-full",
        "jdbc:postgresql://[::1]/barber_booking?sslmode=verify-full",
        "jdbc:postgresql://database.example.invalid/barber_booking?sslmode=verify-full#fragment"
    })
    void rejectsUnverifiedTlsCredentialsInUrlAndLocalDatabases(String database) {
        assertThatThrownBy(() -> safety(database, SSL_FACTORY, ORIGINS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("fixture-only-password");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "org.postgresql.ssl.LibPQFactory", "org.postgresql.ssl.NonValidatingFactory" })
    void rejectsMissingOrUnapprovedSslFactory(String factory) {
        assertThatThrownBy(() -> safety(DATABASE, factory, ORIGINS)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "*", "http://frontend.example.invalid", "https://localhost", "https://127.0.0.2",
        "https://[::1]", "https://frontend.example.invalid/", "https://frontend.example.invalid/path",
        "https://frontend.example.invalid?query=1", "https://frontend.example.invalid#fragment",
        "https://fixture-user:fixture-only-password@frontend.example.invalid",
        "https://frontend.example.invalid,", "https://*.example.invalid"
    })
    void rejectsOriginsWhichCannotBeSafeBrowserOrigins(String origins) {
        assertThatThrownBy(() -> safety(DATABASE, SSL_FACTORY, origins))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("fixture-only-password");
    }

    @Test
    void rejectsDisabledSubscriptionEnforcementAndSandboxPaymentEnvironment() {
        assertThatThrownBy(() -> new ProductionSafetyConfiguration(DATABASE, "fixture_user", "fixture-only-password",
                false, "prod", SSL_FACTORY, ORIGINS)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ProductionSafetyConfiguration(DATABASE, "fixture_user", "fixture-only-password",
                true, "test", SSL_FACTORY, ORIGINS)).isInstanceOf(IllegalStateException.class);
    }

    private static ProductionSafetyConfiguration safety(String database, String sslFactory, String origins) {
        return new ProductionSafetyConfiguration(database, "fixture_user", "fixture-only-password",
                true, "prod", sslFactory, origins);
    }
}
