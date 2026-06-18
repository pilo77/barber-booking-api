package com.villamil.barberbooking.application.dto.response;

import java.time.LocalDateTime;

import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

public record DailyAppointmentItemResponse(
		Long appointmentId,
		Long customerId,
		String customerName,
		Long serviceOfferingId,
		String serviceName,
		LocalDateTime startAt,
		LocalDateTime endAt,
		AppointmentStatus status,
		AppointmentSource source
) {
}
