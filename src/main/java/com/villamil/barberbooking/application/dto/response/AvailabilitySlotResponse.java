package com.villamil.barberbooking.application.dto.response;

import java.time.LocalTime;

import com.villamil.barberbooking.domain.valueobject.AvailabilitySlotStatus;

public record AvailabilitySlotResponse(
		LocalTime startTime,
		LocalTime endTime,
		AvailabilitySlotStatus status,
		String color
) {
}
