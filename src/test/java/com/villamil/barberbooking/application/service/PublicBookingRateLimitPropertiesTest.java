package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class PublicBookingRateLimitPropertiesTest {

	@Test
	void externalConfigurationOverridesDefaults() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
				"booking.public.rate-limit.ip-max-attempts", "25",
				"booking.public.rate-limit.ip-window", "2m",
				"booking.public.rate-limit.phone-max-attempts", "6",
				"booking.public.rate-limit.phone-window", "20m"
		));

		PublicBookingRateLimitProperties properties = new Binder(source)
				.bind(
						"booking.public.rate-limit",
						Bindable.of(PublicBookingRateLimitProperties.class)
				)
				.orElseThrow(() -> new AssertionError("Rate limit properties were not bound"));

		assertThat(properties.getIpMaxAttempts()).isEqualTo(25);
		assertThat(properties.getIpWindow()).isEqualTo(Duration.ofMinutes(2));
		assertThat(properties.getPhoneMaxAttempts()).isEqualTo(6);
		assertThat(properties.getPhoneWindow()).isEqualTo(Duration.ofMinutes(20));
	}
}
