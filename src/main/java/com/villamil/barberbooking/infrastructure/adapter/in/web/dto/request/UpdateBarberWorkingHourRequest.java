package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.villamil.barberbooking.application.dto.command.UpdateBarberWorkingHourCommand;

import jakarta.validation.constraints.NotNull;

public record UpdateBarberWorkingHourRequest(
		@NotNull(message = "Day of week is required")
		DayOfWeek dayOfWeek,

		@NotNull(message = "Start time is required")
		LocalTime startTime,

		@NotNull(message = "End time is required")
		LocalTime endTime
) {

	public UpdateBarberWorkingHourCommand toCommand() {
		return new UpdateBarberWorkingHourCommand(dayOfWeek, startTime, endTime);
	}
}
