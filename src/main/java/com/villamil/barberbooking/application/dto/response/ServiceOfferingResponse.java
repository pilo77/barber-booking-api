package com.villamil.barberbooking.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.villamil.barberbooking.domain.model.ServiceOffering;

public record ServiceOfferingResponse(
		Long id,
		String name,
		String description,
		int durationMinutes,
		BigDecimal price,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public static ServiceOfferingResponse from(ServiceOffering serviceOffering) {
		return new ServiceOfferingResponse(
				serviceOffering.id(),
				serviceOffering.name(),
				serviceOffering.description(),
				serviceOffering.durationMinutes(),
				serviceOffering.price(),
				serviceOffering.active(),
				serviceOffering.createdAt(),
				serviceOffering.updatedAt()
		);
	}
}
