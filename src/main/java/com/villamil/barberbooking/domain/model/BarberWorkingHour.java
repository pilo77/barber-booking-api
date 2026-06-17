package com.villamil.barberbooking.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.valueobject.TimeRange;

public record BarberWorkingHour(
		Long id,
		Long barberId,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		boolean active
) {

	public BarberWorkingHour {
		validateId(id, "Working hour id");
		barberId = requirePositive(barberId, "Barber id is required");
		Objects.requireNonNull(dayOfWeek, "Day of week is required");
		Objects.requireNonNull(startTime, "Start time is required");
		Objects.requireNonNull(endTime, "End time is required");
		if (!startTime.isBefore(endTime)) {
			throw new BusinessRuleException("Working hour start time must be before end time");
		}
	}

	public TimeRange range() {
		return new TimeRange(startTime, endTime);
	}

	private static Long requirePositive(Long value, String message) {
		if (value == null || value <= 0) {
			throw new BusinessRuleException(message);
		}
		return value;
	}

	private static void validateId(Long value, String label) {
		if (value != null && value <= 0) {
			throw new BusinessRuleException(label + " must be positive");
		}
	}
}
