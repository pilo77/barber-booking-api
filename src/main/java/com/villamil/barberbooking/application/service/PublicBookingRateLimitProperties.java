package com.villamil.barberbooking.application.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "booking.public.rate-limit")
class PublicBookingRateLimitProperties {

	private int ipMaxAttempts = 10;
	private Duration ipWindow = Duration.ofMinutes(1);
	private int phoneMaxAttempts = 3;
	private Duration phoneWindow = Duration.ofMinutes(10);

	int getIpMaxAttempts() {
		return ipMaxAttempts;
	}

	void setIpMaxAttempts(int ipMaxAttempts) {
		this.ipMaxAttempts = ipMaxAttempts;
	}

	Duration getIpWindow() {
		return ipWindow;
	}

	void setIpWindow(Duration ipWindow) {
		this.ipWindow = ipWindow;
	}

	int getPhoneMaxAttempts() {
		return phoneMaxAttempts;
	}

	void setPhoneMaxAttempts(int phoneMaxAttempts) {
		this.phoneMaxAttempts = phoneMaxAttempts;
	}

	Duration getPhoneWindow() {
		return phoneWindow;
	}

	void setPhoneWindow(Duration phoneWindow) {
		this.phoneWindow = phoneWindow;
	}
}
