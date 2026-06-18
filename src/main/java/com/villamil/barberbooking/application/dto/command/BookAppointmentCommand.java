package com.villamil.barberbooking.application.dto.command;

import java.time.LocalDateTime;

public record BookAppointmentCommand(
		Long customerId,
		Long barberId,
		Long serviceOfferingId,
		LocalDateTime startAt
) {
}
