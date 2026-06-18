package com.villamil.barberbooking.application.dto.response;

public record BarberDailyDashboardSummaryResponse(
		int totalAppointments,
		int scheduled,
		int inProgress,
		int completed,
		int cancelled,
		int noShow,
		long occupiedMinutes
) {
}
