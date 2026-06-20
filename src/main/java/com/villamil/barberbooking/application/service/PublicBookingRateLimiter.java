package com.villamil.barberbooking.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.villamil.barberbooking.application.exception.PublicBookingRateLimitExceededException;

@Service
class PublicBookingRateLimiter {

	private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
	private final PublicBookingRateLimitProperties properties;
	private final Clock clock;

	PublicBookingRateLimiter(PublicBookingRateLimitProperties properties) {
		this(properties, Clock.systemUTC());
	}

	PublicBookingRateLimiter(PublicBookingRateLimitProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	void check(String remoteAddress, Long companyId, Long branchId, String phone) {
		consume(
				"ip:" + normalize(remoteAddress),
				properties.getIpMaxAttempts(),
				properties.getIpWindow(),
				"Too many public booking attempts from this address"
		);
		consume(
				"phone:" + companyId + ":" + branchId + ":" + normalize(phone),
				properties.getPhoneMaxAttempts(),
				properties.getPhoneWindow(),
				"Too many public booking attempts for this phone"
		);
	}

	private void consume(String key, int limit, Duration window, String message) {
		Instant now = clock.instant();
		AttemptWindow attemptWindow = attempts.computeIfAbsent(key, ignored -> new AttemptWindow());
		synchronized (attemptWindow) {
			Instant cutoff = now.minus(window);
			while (!attemptWindow.timestamps.isEmpty()
					&& !attemptWindow.timestamps.peekFirst().isAfter(cutoff)) {
				attemptWindow.timestamps.removeFirst();
			}
			if (attemptWindow.timestamps.size() >= limit) {
				throw new PublicBookingRateLimitExceededException(message);
			}
			attemptWindow.timestamps.addLast(now);
		}
	}

	private String normalize(String value) {
		return value == null ? "unknown" : value.strip().toLowerCase(Locale.ROOT);
	}

	private static final class AttemptWindow {
		private final Deque<Instant> timestamps = new ArrayDeque<>();
	}
}
