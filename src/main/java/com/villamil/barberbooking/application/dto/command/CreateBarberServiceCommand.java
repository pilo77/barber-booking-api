package com.villamil.barberbooking.application.dto.command;

import java.math.BigDecimal;

public record CreateBarberServiceCommand(
		String name,
		int durationMinutes,
		BigDecimal price
) {
}
