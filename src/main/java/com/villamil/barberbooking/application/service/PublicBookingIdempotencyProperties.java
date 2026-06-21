package com.villamil.barberbooking.application.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "booking.public.idempotency")
public class PublicBookingIdempotencyProperties {

	private Duration inProgressTtl = Duration.ofMinutes(2);

	public Duration getInProgressTtl() {
		return inProgressTtl;
	}

	public void setInProgressTtl(Duration inProgressTtl) {
		this.inProgressTtl = inProgressTtl;
	}

	public long getInProgressTtlSeconds() {
		return Math.max(1L, inProgressTtl.toSeconds());
	}
}
