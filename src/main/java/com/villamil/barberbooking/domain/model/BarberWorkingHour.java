package com.villamil.barberbooking.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.valueobject.TimeRange;

public record BarberWorkingHour(
		Long id,
		Long barberId,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public BarberWorkingHour {
		validateId(id, "Working hour id");
		barberId = requirePositive(barberId, "Barber id is required");
		dayOfWeek = requireValue(dayOfWeek, "Day of week is required");
		startTime = requireValue(startTime, "Start time is required");
		endTime = requireValue(endTime, "End time is required");
		if (!startTime.isBefore(endTime)) {
			throw new BusinessRuleException("Working hour start time must be before end time");
		}
		createdAt = createdAt == null ? Instant.now() : createdAt;
		updatedAt = updatedAt == null ? createdAt : updatedAt;
	}

	public static BarberWorkingHour create(
			Long barberId,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			LocalTime endTime
	) {
		Instant now = Instant.now();
		return new BarberWorkingHour(null, barberId, dayOfWeek, startTime, endTime, true, now, now);
	}

	public BarberWorkingHour update(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
		return new BarberWorkingHour(id, barberId, dayOfWeek, startTime, endTime, active, createdAt, Instant.now());
	}

	public BarberWorkingHour activate() {
		if (active) {
			return this;
		}
		return new BarberWorkingHour(id, barberId, dayOfWeek, startTime, endTime, true, createdAt, Instant.now());
	}

	public BarberWorkingHour deactivate() {
		if (!active) {
			return this;
		}
		return new BarberWorkingHour(id, barberId, dayOfWeek, startTime, endTime, false, createdAt, Instant.now());
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

	private static <T> T requireValue(T value, String message) {
		if (value == null) {
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
