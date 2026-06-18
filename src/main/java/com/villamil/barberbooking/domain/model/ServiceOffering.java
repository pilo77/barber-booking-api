package com.villamil.barberbooking.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record ServiceOffering(
		Long id,
		String name,
		String description,
		int durationMinutes,
		BigDecimal price,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public ServiceOffering {
		validateId(id, "Service offering id");
		name = requireText(name, "Service offering name is required");
		description = normalizeOptionalText(description);
		if (durationMinutes <= 0) {
			throw new BusinessRuleException("Service offering duration must be positive");
		}
		price = normalizePrice(price);
		createdAt = createdAt == null ? Instant.now() : createdAt;
		updatedAt = updatedAt == null ? createdAt : updatedAt;
	}

	public static ServiceOffering create(String name, String description, int durationMinutes, BigDecimal price) {
		Instant now = Instant.now();
		return new ServiceOffering(null, name, description, durationMinutes, price, true, now, now);
	}

	public ServiceOffering update(String name, String description, int durationMinutes, BigDecimal price) {
		return new ServiceOffering(id, name, description, durationMinutes, price, active, createdAt, Instant.now());
	}

	public ServiceOffering activate() {
		if (active) {
			return this;
		}
		return new ServiceOffering(id, name, description, durationMinutes, price, true, createdAt, Instant.now());
	}

	public ServiceOffering deactivate() {
		if (!active) {
			return this;
		}
		return new ServiceOffering(id, name, description, durationMinutes, price, false, createdAt, Instant.now());
	}

	private static String requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(message);
		}
		return value.strip();
	}

	private static String normalizeOptionalText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}

	private static BigDecimal normalizePrice(BigDecimal value) {
		if (value == null || value.signum() < 0) {
			throw new BusinessRuleException("Service offering price must be zero or positive");
		}
		try {
			return value.setScale(2, RoundingMode.UNNECESSARY);
		}
		catch (ArithmeticException exception) {
			throw new BusinessRuleException("Service offering price must have at most 2 decimal places");
		}
	}

	private static void validateId(Long value, String label) {
		if (value != null && value <= 0) {
			throw new BusinessRuleException(label + " must be positive");
		}
	}
}
