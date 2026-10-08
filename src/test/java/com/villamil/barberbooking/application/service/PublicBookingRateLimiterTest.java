package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.villamil.barberbooking.application.exception.PublicBookingRateLimitExceededException;

class PublicBookingRateLimiterTest {

	private PublicBookingRateLimiter limiter;
	private PublicBookingRateLimitProperties properties;

	@BeforeEach
	void setUp() {
		properties = new PublicBookingRateLimitProperties();
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

	@Test
	void fullCapacityRejectsNewIdentitiesWithoutEvictingActivePhoneLimits() {
		limiter = new PublicBookingRateLimiter(properties, Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneOffset.UTC), 4);
		limiter.check("203.0.113.1", 1L, 1L, "3000000001");
		limiter.check("203.0.113.2", 1L, 1L, "3000000002");

		assertThatThrownBy(() -> limiter.check("203.0.113.3", 1L, 1L, "3000000003"))
				.isInstanceOf(PublicBookingRateLimitExceededException.class);
		limiter.check("203.0.113.1", 1L, 1L, "3000000001");
		limiter.check("203.0.113.1", 1L, 1L, "3000000001");
		assertThatThrownBy(() -> limiter.check("203.0.113.1", 1L, 1L, "3000000001"))
				.isInstanceOf(PublicBookingRateLimitExceededException.class)
				.hasMessageContaining("phone");
	}

	@Test
	void expiredAddressesReleaseCapacityButLongerPhoneLimitsRemainActive() {
		MutableClock clock = new MutableClock(Instant.parse("2026-06-20T12:00:00Z"));
		limiter = new PublicBookingRateLimiter(properties, clock, 4);
		limiter.check("203.0.113.1", 1L, 1L, "3000000001");
		limiter.check("203.0.113.2", 1L, 1L, "3000000002");
		clock.advance(Duration.ofMinutes(1));

		assertThatCode(() -> limiter.check("203.0.113.3", 1L, 1L, "3000000001"))
				.doesNotThrowAnyException();
		assertThatCode(() -> limiter.check("203.0.113.4", 1L, 1L, "3000000001"))
				.doesNotThrowAnyException();
		assertThatThrownBy(() -> limiter.check("203.0.113.3", 1L, 1L, "3000000001"))
				.isInstanceOf(PublicBookingRateLimitExceededException.class)
				.hasMessageContaining("phone");
	}

	@Test
	void retryAtExactWindowExpirySucceeds() {
		properties.setIpMaxAttempts(1);
		properties.setPhoneMaxAttempts(1);
		properties.setIpWindow(Duration.ofSeconds(10));
		properties.setPhoneWindow(Duration.ofSeconds(10));
		MutableClock clock = new MutableClock(Instant.parse("2026-06-20T12:00:00Z"));
		limiter = new PublicBookingRateLimiter(properties, clock, 2);
		limiter.check("203.0.113.1", 1L, 1L, "3000000001");
		assertThatThrownBy(() -> limiter.check("203.0.113.1", 1L, 1L, "3000000001"))
				.isInstanceOf(PublicBookingRateLimitExceededException.class);

		clock.advance(Duration.ofSeconds(10));
		assertThatCode(() -> limiter.check("203.0.113.2", 1L, 1L, "3000000002"))
				.doesNotThrowAnyException();
		clock.advance(Duration.ofSeconds(10));
		assertThatCode(() -> limiter.check("203.0.113.1", 1L, 1L, "3000000001"))
				.doesNotThrowAnyException();
	}

	@Test
	void simultaneousAttemptsCannotBypassTheAddressLimit() throws Exception {
		try (var workers = Executors.newFixedThreadPool(8)) {
			var tasks = new ArrayList<Callable<Boolean>>();
			for (int index = 0; index < 30; index++) {
				String phone = "3000000" + index;
				tasks.add(() -> {
					try {
						limiter.check("203.0.113.1", 1L, 1L, phone);
						return true;
					} catch (PublicBookingRateLimitExceededException expected) {
						return false;
					}
				});
			}
			int accepted = 0;
			for (Future<Boolean> result : workers.invokeAll(tasks)) {
				if (result.get()) accepted++;
			}
			assertThat(accepted).isEqualTo(10);
		}
	}

	private static final class MutableClock extends Clock {
		private Instant current;
		private MutableClock(Instant current) { this.current = current; }
		private void advance(Duration duration) { current = current.plus(duration); }
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return Clock.fixed(current, zone); }
		@Override public Instant instant() { return current; }
	}
}
