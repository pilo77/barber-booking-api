package com.villamil.barberbooking.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.villamil.barberbooking.application.exception.PublicBookingRateLimitExceededException;

@Service
class PublicBookingRateLimiter {

	private static final int MAX_TRACKED_KEYS = 10_000;
	private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(1);
	private final Map<String, AttemptWindow> attempts = new HashMap<>();
	private final PublicBookingRateLimitProperties properties;
	private final Clock clock;
	private final int maxTrackedKeys;
	private Instant nextCleanupAt = Instant.MIN;

	@Autowired
	PublicBookingRateLimiter(PublicBookingRateLimitProperties properties) {
		this(properties, Clock.systemUTC());
	}

	PublicBookingRateLimiter(PublicBookingRateLimitProperties properties, Clock clock) {
		this(properties, clock, MAX_TRACKED_KEYS);
	}

	PublicBookingRateLimiter(PublicBookingRateLimitProperties properties, Clock clock, int maxTrackedKeys) {
		if (maxTrackedKeys < 1) throw new IllegalArgumentException("Rate limiter capacity must be positive");
		this.properties = properties;
		this.clock = clock;
		this.maxTrackedKeys = maxTrackedKeys;
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

	private synchronized void consume(String key, int limit, Duration window, String message) {
		Instant now = clock.instant();
		if (!now.isBefore(nextCleanupAt)) {
			removeExpired(now);
			nextCleanupAt = now.plus(CLEANUP_INTERVAL);
		}
		AttemptWindow attemptWindow = attempts.get(key);
		if (attemptWindow == null) {
			if (attempts.size() >= maxTrackedKeys) removeExpired(now);
			// Preserve active limits instead of evicting them when new identities fill the map.
			if (attempts.size() >= maxTrackedKeys) throw new PublicBookingRateLimitExceededException(message);
			attemptWindow = new AttemptWindow();
			attempts.put(key, attemptWindow);
		}
		Instant cutoff = now.minus(window);
		while (!attemptWindow.timestamps.isEmpty()
				&& !attemptWindow.timestamps.peekFirst().isAfter(cutoff)) {
			attemptWindow.timestamps.removeFirst();
		}
		if (attemptWindow.timestamps.size() >= limit) {
			throw new PublicBookingRateLimitExceededException(message);
		}
		attemptWindow.timestamps.addLast(now);
		attemptWindow.expiresAt = now.plus(window);
	}

	private void removeExpired(Instant now) {
		attempts.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(now));
	}

	private String normalize(String value) {
		return value == null ? "unknown" : value.strip().toLowerCase(Locale.ROOT);
	}

	private static final class AttemptWindow {
		private final Deque<Instant> timestamps = new ArrayDeque<>();
		private Instant expiresAt = Instant.MIN;
	}
}
