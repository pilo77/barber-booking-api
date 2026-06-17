package com.villamil.barberbooking.application.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;

import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

public record AppointmentResponse(
		Long id,
		Long customerId,
		Long barberId,
		Long serviceId,
		LocalDateTime startAt,
		LocalDateTime endAt,
		AppointmentStatus status,
		AppointmentSource source,
		Instant createdAt
) {

	public static AppointmentResponse from(Appointment appointment) {
		return new AppointmentResponse(
				appointment.id(),
				appointment.customerId(),
				appointment.barberId(),
				appointment.serviceId(),
				appointment.startAt(),
				appointment.endAt(),
				appointment.status(),
				appointment.source(),
				appointment.createdAt()
		);
	}
}
