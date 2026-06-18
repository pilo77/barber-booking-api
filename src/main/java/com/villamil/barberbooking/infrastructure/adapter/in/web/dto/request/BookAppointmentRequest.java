package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import java.time.LocalDateTime;

import com.villamil.barberbooking.application.dto.command.BookAppointmentCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookAppointmentRequest(
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
		LocalDateTime startAt
) {

	public BookAppointmentCommand toCommand() {
		return new BookAppointmentCommand(customerId, barberId, serviceOfferingId, startAt);
	}
}
