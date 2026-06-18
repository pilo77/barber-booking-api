package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.port.in.GetAppointmentUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;

@Service
class GetAppointmentService implements GetAppointmentUseCase {

	private final AppointmentRepositoryPort appointmentRepositoryPort;

	GetAppointmentService(AppointmentRepositoryPort appointmentRepositoryPort) {
		this.appointmentRepositoryPort = appointmentRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public AppointmentResponse getById(Long id) {
		return appointmentRepositoryPort.findById(id)
				.map(AppointmentResponse::from)
				.orElseThrow(() -> new AppointmentNotFoundException("Appointment not found"));
	}
}
