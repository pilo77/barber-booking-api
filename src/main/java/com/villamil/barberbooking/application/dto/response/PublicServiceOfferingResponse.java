package com.villamil.barberbooking.application.dto.response;

import java.math.BigDecimal;

public record PublicServiceOfferingResponse(
		Long id,
		String name,
		String description,
		int durationMinutes,
		BigDecimal price
) {
}
