package com.villamil.barberbooking.application.dto.response;

import java.time.LocalDate;
import java.util.List;

public record BarberDailyDashboardResponse(
		Long barberId,
		String barberName,
		LocalDate date,
		BarberDailyDashboardSummaryResponse summary,
		DailyAppointmentItemResponse nextAppointment,
		List<DailyAppointmentItemResponse> appointments
) {

	public BarberDailyDashboardResponse {
		appointments = appointments == null ? List.of() : List.copyOf(appointments);
	}
}
