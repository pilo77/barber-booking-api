package com.villamil.barberbooking.application.dto.response;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import com.villamil.barberbooking.domain.model.BarberWorkingHour;

public record BarberWorkingHourResponse(
		Long id,
		Long barberId,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public static BarberWorkingHourResponse from(BarberWorkingHour workingHour) {
		return new BarberWorkingHourResponse(
				workingHour.id(),
				workingHour.barberId(),
				workingHour.dayOfWeek(),
				workingHour.startTime(),
				workingHour.endTime(),
				workingHour.active(),
				workingHour.createdAt(),
				workingHour.updatedAt()
		);
	}
}
