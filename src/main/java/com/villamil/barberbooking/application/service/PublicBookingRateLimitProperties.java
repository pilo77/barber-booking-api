package com.villamil.barberbooking.application.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "booking.public.rate-limit")
public class PublicBookingRateLimitProperties {

	private int ipMaxAttempts = 10;
	private Duration ipWindow = Duration.ofMinutes(1);
	private int phoneMaxAttempts = 3;
	private Duration phoneWindow = Duration.ofMinutes(10);

	public int getIpMaxAttempts() {
		return ipMaxAttempts;
	}

	public void setIpMaxAttempts(int ipMaxAttempts) {
		this.ipMaxAttempts = ipMaxAttempts;
	}

	public Duration getIpWindow() {
		return ipWindow;
	}

	public void setIpWindow(Duration ipWindow) {
		this.ipWindow = ipWindow;
	}

	public int getPhoneMaxAttempts() {
		return phoneMaxAttempts;
	}

	public void setPhoneMaxAttempts(int phoneMaxAttempts) {
		this.phoneMaxAttempts = phoneMaxAttempts;
	}

	public Duration getPhoneWindow() {
		return phoneWindow;
	}

	public void setPhoneWindow(Duration phoneWindow) {
		this.phoneWindow = phoneWindow;
	}
}
