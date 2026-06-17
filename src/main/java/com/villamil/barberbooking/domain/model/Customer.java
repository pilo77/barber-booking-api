package com.villamil.barberbooking.domain.model;

import java.time.Instant;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record Customer(
		Long id,
		String fullName,
		String phone,
		String email,
		Instant createdAt
) {

	public Customer {
		validateId(id, "Customer id");
		fullName = requireText(fullName, "Customer full name is required");
		phone = requireText(phone, "Customer phone is required");
		email = normalizeOptionalText(email);
		createdAt = createdAt == null ? Instant.now() : createdAt;
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
