package com.villamil.barberbooking.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

public record PublicAppointmentResponse(
		Long id,
		AppointmentStatus status,
		AppointmentSource source,
		LocalDateTime startAt,
		LocalDateTime endAt,
		ServiceSummary service,
		BarberSummary barber,
		CustomerSummary customer
) {

	public static PublicAppointmentResponse from(
			Appointment appointment,
			PublicServiceOfferingResponse service,
			PublicBarberResponse barber,
			String customerFullName,
			String customerPhone
	) {
		return new PublicAppointmentResponse(
				appointment.id(),
				appointment.status(),
				appointment.source(),
				appointment.startAt(),
				appointment.endAt(),
				new ServiceSummary(service.id(), service.name(), service.durationMinutes(), service.price()),
				new BarberSummary(barber.id(), barber.displayName(), barber.photoUrl()),
				new CustomerSummary(customerFullName, customerPhone)
		);
	}

	public record ServiceSummary(Long id, String name, int durationMinutes, BigDecimal price) {
	}

	public record BarberSummary(Long id, String displayName, String photoUrl) {
	}

	public record CustomerSummary(String fullName, String phone) {
	}
}
