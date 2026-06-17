package com.villamil.barberbooking.application.dto.response;

import java.time.LocalDate;
import java.util.List;

public record BarberDailyScheduleResponse(
		Long barberId,
		LocalDate date,
		List<AppointmentResponse> appointments
) {
}
