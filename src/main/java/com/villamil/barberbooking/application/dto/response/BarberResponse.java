package com.villamil.barberbooking.application.dto.response;

import java.time.Instant;

import com.villamil.barberbooking.domain.model.Barber;

public record BarberResponse(
		Long id,
		String fullName,
		String phone,
		String email,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public static BarberResponse from(Barber barber) {
		return new BarberResponse(
				barber.id(),
				barber.fullName(),
				barber.phone(),
				barber.email(),
				barber.active(),
				barber.createdAt(),
				barber.updatedAt()
		);
	}
}
