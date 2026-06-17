package com.villamil.barberbooking.application.dto.response;

import java.math.BigDecimal;

import com.villamil.barberbooking.domain.model.BarberService;

public record BarberServiceResponse(
		Long id,
		String name,
		int durationMinutes,
		BigDecimal price,
		boolean active
) {

	public static BarberServiceResponse from(BarberService service) {
		return new BarberServiceResponse(
				service.id(),
				service.name(),
				service.durationMinutes(),
				service.price(),
				service.active()
		);
	}
}
