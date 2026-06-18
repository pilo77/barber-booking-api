package com.villamil.barberbooking.application.dto.response;

import java.time.LocalDate;
import java.util.List;

public record BarberAvailabilityResponse(
		Long barberId,
		Long serviceOfferingId,
		LocalDate date,
		List<AvailabilitySlotResponse> slots
) {

	public BarberAvailabilityResponse {
		slots = slots == null ? List.of() : List.copyOf(slots);
	}
}
