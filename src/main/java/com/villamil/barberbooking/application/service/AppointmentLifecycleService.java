package com.villamil.barberbooking.application.service;

import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.port.in.CompleteAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.MarkAppointmentNoShowUseCase;
import com.villamil.barberbooking.application.port.in.StartAppointmentUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;

@Service
class AppointmentLifecycleService implements
		StartAppointmentUseCase,
		CompleteAppointmentUseCase,
		MarkAppointmentNoShowUseCase {

	private final AppointmentRepositoryPort appointmentRepositoryPort;

	AppointmentLifecycleService(AppointmentRepositoryPort appointmentRepositoryPort) {
		this.appointmentRepositoryPort = appointmentRepositoryPort;
	}

	@Override
	@Transactional
	public AppointmentResponse start(Long id) {
		return changeStatus(id, Appointment::start);
	}

	@Override
	@Transactional
	public AppointmentResponse complete(Long id) {
		return changeStatus(id, Appointment::complete);
	}

	@Override
	@Transactional
	public AppointmentResponse markNoShow(Long id) {
		return changeStatus(id, Appointment::markNoShow);
	}

	private AppointmentResponse changeStatus(Long id, Function<Appointment, Appointment> transition) {
		Appointment appointment = appointmentRepositoryPort.findById(id)
				.orElseThrow(() -> new AppointmentNotFoundException("Appointment not found"));
		return AppointmentResponse.from(appointmentRepositoryPort.save(transition.apply(appointment)));
	}
}
