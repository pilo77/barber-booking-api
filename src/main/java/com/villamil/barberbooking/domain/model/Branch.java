package com.villamil.barberbooking.domain.model;

import java.time.Instant;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record Branch(
		Long id,
		Long companyId,
		String name,
		String slug,
		String address,
		String phone,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public Branch {
		validateId(id, "Branch id");
		companyId = requirePositive(companyId, "Company id is required");
		name = requireText(name, "Branch name is required");
		slug = requireText(slug, "Branch slug is required");
		address = normalizeOptionalText(address);
		phone = normalizeOptionalText(phone);
		createdAt = createdAt == null ? Instant.now() : createdAt;
		updatedAt = updatedAt == null ? createdAt : updatedAt;
	}

	private static Long requirePositive(Long value, String message) {
		if (value == null || value <= 0) {
			throw new BusinessRuleException(message);
		}
		return value;
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
