package com.villamil.barberbooking.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ApplicationTimeConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(ApplicationTimeConfiguration.class);

    @Test void defaultBusinessClockUsesColombiaIndependentlyOfJvmZone() {
        context.run(application -> {
            var clock = application.getBean(java.time.Clock.class);
            assertEquals(ZoneId.of("America/Bogota"), clock.getZone());
            assertEquals(LocalTime.of(9, 0), ZonedDateTime.parse("2026-10-08T14:00:00Z")
                    .withZoneSameInstant(clock.getZone()).toLocalTime());
        });
    }

    @Test void businessZoneCanBeConfiguredExplicitly() {
        context.withPropertyValues("booking.time-zone=America/Lima").run(application ->
                assertEquals(ZoneId.of("America/Lima"), application.getBean(java.time.Clock.class).getZone()));
    }

    @Test void invalidZoneFailsRatherThanUsingAnUnexpectedServerZone() {
        assertThrows(DateTimeException.class, () -> new ApplicationTimeConfiguration().bookingClock("Invalid/Zone"));
    }
}
