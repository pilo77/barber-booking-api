package com.villamil.barberbooking.domain.model;

import java.time.Instant;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record Company(
		Long id,
		String name,
		String slug,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public Company {
		validateId(id, "Company id");
		name = requireText(name, "Company name is required");
		slug = requireText(slug, "Company slug is required");
		createdAt = createdAt == null ? Instant.now() : createdAt;
		updatedAt = updatedAt == null ? createdAt : updatedAt;
	}

	private static String requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(message);
		}
		return value.strip();
	}

	private static void validateId(Long value, String label) {
		if (value != null && value <= 0) {
			throw new BusinessRuleException(label + " must be positive");
		}
	}
}
