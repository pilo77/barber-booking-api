package com.villamil.barberbooking.application.dto.command;

import java.time.LocalDateTime;

import com.villamil.barberbooking.domain.valueobject.AppointmentSource;

public record BookAppointmentCommand(
		Long customerId,
		Long barberId,
		Long serviceId,
		LocalDateTime startAt,
		AppointmentSource source
) {
}
