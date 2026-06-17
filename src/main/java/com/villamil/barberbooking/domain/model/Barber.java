package com.villamil.barberbooking.domain.model;

import java.time.Instant;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record Barber(
		Long id,
		String fullName,
		String phone,
		boolean active,
		Instant createdAt
) {

	public Barber {
		validateId(id, "Barber id");
		fullName = requireText(fullName, "Barber full name is required");
		phone = normalizeOptionalText(phone);
		createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	public Barber activate() {
		return new Barber(id, fullName, phone, true, createdAt);
	}

	public Barber deactivate() {
		return new Barber(id, fullName, phone, false, createdAt);
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

	private static void validateId(Long value, String label) {
		if (value != null && value <= 0) {
			throw new BusinessRuleException(label + " must be positive");
		}
	}
}
