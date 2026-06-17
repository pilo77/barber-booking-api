package com.villamil.barberbooking.application.dto.response;

import java.time.Instant;

import com.villamil.barberbooking.domain.model.Barber;

public record BarberResponse(
		Long id,
		String fullName,
		String phone,
		boolean active,
		Instant createdAt
) {

	public static BarberResponse from(Barber barber) {
		return new BarberResponse(
				barber.id(),
				barber.fullName(),
				barber.phone(),
				barber.active(),
				barber.createdAt()
		);
	}
}
