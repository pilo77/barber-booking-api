package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import java.time.LocalDateTime;

import com.villamil.barberbooking.application.dto.command.CreateWalkInAppointmentCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateWalkInAppointmentRequest(
		@NotNull(message = "Customer id is required")
		@Positive(message = "Customer id must be positive")
		Long customerId,

		@NotNull(message = "Barber id is required")
		@Positive(message = "Barber id must be positive")
		Long barberId,

		@NotNull(message = "Service offering id is required")
		@Positive(message = "Service offering id must be positive")
		Long serviceOfferingId,

		@NotNull(message = "Appointment start date is required")
		LocalDateTime startAt,

		@NotNull(message = "Start immediately flag is required")
		Boolean startImmediately
) {

	public CreateWalkInAppointmentCommand toCommand() {
		return new CreateWalkInAppointmentCommand(
				customerId,
				barberId,
				serviceOfferingId,
				startAt,
				startImmediately
		);
	}
}
