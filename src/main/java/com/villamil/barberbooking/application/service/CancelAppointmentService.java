package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.port.in.CancelAppointmentUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;

@Service
class CancelAppointmentService implements CancelAppointmentUseCase {

	private final AppointmentRepositoryPort appointmentRepositoryPort;

	CancelAppointmentService(AppointmentRepositoryPort appointmentRepositoryPort) {
		this.appointmentRepositoryPort = appointmentRepositoryPort;
	}

	@Override
	@Transactional
	public AppointmentResponse cancel(Long id) {
		Appointment appointment = appointmentRepositoryPort.findById(id)
				.orElseThrow(() -> new AppointmentNotFoundException("Appointment not found"));

		return AppointmentResponse.from(appointmentRepositoryPort.save(appointment.cancel()));
	}
}
