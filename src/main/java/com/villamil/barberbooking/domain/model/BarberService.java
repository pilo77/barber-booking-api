package com.villamil.barberbooking.domain.model;

import java.math.BigDecimal;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record BarberService(
		Long id,
		String name,
		int durationMinutes,
		BigDecimal price,
		boolean active
) {

	public BarberService {
		validateId(id, "Service id");
		name = requireText(name, "Service name is required");
		if (durationMinutes <= 0) {
			throw new BusinessRuleException("Service duration must be positive");
		}
		if (price == null || price.signum() < 0) {
			throw new BusinessRuleException("Service price must be zero or positive");
		}
		price = price.stripTrailingZeros();
	}

	public BarberService activate() {
		return new BarberService(id, name, durationMinutes, price, true);
	}

	public BarberService deactivate() {
		return new BarberService(id, name, durationMinutes, price, false);
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
