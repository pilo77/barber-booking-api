package com.villamil.barberbooking.application.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.dto.response.BarberDailyScheduleResponse;
import com.villamil.barberbooking.application.port.in.GetBarberDailyAppointmentsUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;

@Service
class GetBarberDailyAppointmentsService implements GetBarberDailyAppointmentsUseCase {

	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final BarberRepositoryPort barberRepositoryPort;

	GetBarberDailyAppointmentsService(
			AppointmentRepositoryPort appointmentRepositoryPort,
			BarberRepositoryPort barberRepositoryPort
	) {
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.barberRepositoryPort = barberRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public BarberDailyScheduleResponse getDailyAppointments(Long barberId, LocalDate date) {
		barberRepositoryPort.findById(barberId)
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));

		return new BarberDailyScheduleResponse(
				barberId,
				date,
				appointmentRepositoryPort.findByBarberIdAndDate(barberId, date)
						.stream()
						.map(AppointmentResponse::from)
						.toList()
		);
	}
}
