package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.villamil.barberbooking.application.exception.PublicBookingRateLimitExceededException;

class PublicBookingRateLimiterTest {

	private PublicBookingRateLimiter limiter;

	@BeforeEach
	void setUp() {
		PublicBookingRateLimitProperties properties = new PublicBookingRateLimitProperties();
		properties.setIpMaxAttempts(10);
		properties.setIpWindow(Duration.ofMinutes(1));
		properties.setPhoneMaxAttempts(3);
		properties.setPhoneWindow(Duration.ofMinutes(10));
		limiter = new PublicBookingRateLimiter(
				properties,
				Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneOffset.UTC)
		);
	}

	@Test
	void exceedingIpLimitReturnsTooManyRequestsException() {
		for (int index = 0; index < 10; index++) {
			limiter.check("203.0.113.10", 1L, index + 1L, "30000000" + index);
		}

		assertThatThrownBy(() -> limiter.check("203.0.113.10", 1L, 99L, "3999999999"))
				.isInstanceOf(PublicBookingRateLimitExceededException.class)
				.hasMessageContaining("address");
	}

	@Test
	void exceedingPhoneLimitIsScopedByCompanyAndBranch() {
		for (int index = 0; index < 3; index++) {
			limiter.check("203.0.113." + index, 1L, 2L, "3001234567");
		}

		assertThatThrownBy(() -> limiter.check("203.0.113.20", 1L, 2L, "3001234567"))
				.isInstanceOf(PublicBookingRateLimitExceededException.class)
				.hasMessageContaining("phone");
		assertThatCode(() -> limiter.check("203.0.113.21", 1L, 3L, "3001234567"))
				.doesNotThrowAnyException();
		assertThatCode(() -> limiter.check("203.0.113.22", 2L, 2L, "3001234567"))
				.doesNotThrowAnyException();
	}
}
