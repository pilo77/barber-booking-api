package com.villamil.barberbooking.application.dto.command;

import java.time.LocalDate;
import java.util.Objects;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record GetBarberDailyDashboardCommand(
		Long barberId,
		LocalDate date
) {

	public GetBarberDailyDashboardCommand {
		barberId = requirePositive(barberId, "Barber id is required");
		Objects.requireNonNull(date, "Dashboard date is required");
	}

	private static Long requirePositive(Long value, String message) {
		if (value == null || value <= 0) {
			throw new BusinessRuleException(message);
		}
		return value;
	}
}
