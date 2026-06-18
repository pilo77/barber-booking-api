package com.villamil.barberbooking.application.dto.command;

import java.math.BigDecimal;

public record UpdateServiceOfferingCommand(
		String name,
		String description,
		int durationMinutes,
		BigDecimal price
) {
}
