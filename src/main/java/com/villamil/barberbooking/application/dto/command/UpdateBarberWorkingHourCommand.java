package com.villamil.barberbooking.application.dto.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateBarberWorkingHourCommand(
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime
) {
}
