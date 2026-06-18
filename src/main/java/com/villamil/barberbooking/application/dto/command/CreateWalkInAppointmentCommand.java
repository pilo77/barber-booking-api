package com.villamil.barberbooking.application.dto.command;

import java.time.LocalDateTime;

public record CreateWalkInAppointmentCommand(
		Long customerId,
		Long barberId,
		Long serviceOfferingId,
		LocalDateTime startAt,
		boolean startImmediately
) {
}
