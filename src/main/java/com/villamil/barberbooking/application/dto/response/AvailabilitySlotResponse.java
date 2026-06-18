package com.villamil.barberbooking.application.dto.response;

import java.time.LocalDateTime;
import java.util.Objects;

import com.villamil.barberbooking.domain.valueobject.AvailabilitySlotColor;
import com.villamil.barberbooking.domain.valueobject.AvailabilitySlotStatus;

public record AvailabilitySlotResponse(
		LocalDateTime startAt,
		LocalDateTime endAt,
		AvailabilitySlotStatus status,
		AvailabilitySlotColor color,
		boolean available
) {

	public AvailabilitySlotResponse {
		Objects.requireNonNull(startAt, "Slot start date is required");
		Objects.requireNonNull(endAt, "Slot end date is required");
		Objects.requireNonNull(status, "Slot status is required");
		Objects.requireNonNull(color, "Slot color is required");
	}

	public static AvailabilitySlotResponse available(LocalDateTime startAt, LocalDateTime endAt) {
		return new AvailabilitySlotResponse(
				startAt,
				endAt,
				AvailabilitySlotStatus.AVAILABLE,
				AvailabilitySlotColor.GREEN,
				true
		);
	}

	public static AvailabilitySlotResponse occupied(LocalDateTime startAt, LocalDateTime endAt) {
		return new AvailabilitySlotResponse(
				startAt,
				endAt,
				AvailabilitySlotStatus.OCCUPIED,
				AvailabilitySlotColor.RED,
				false
		);
	}
}
