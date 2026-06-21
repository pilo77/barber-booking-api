package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.port.in.CancelAppointmentUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;
import com.villamil.barberbooking.domain.model.Appointment;

@Service
class CancelAppointmentService implements CancelAppointmentUseCase {

	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final CurrentUserResolver currentUserResolver;
	private final UserAuthorizationPolicy userAuthorizationPolicy;

	CancelAppointmentService(AppointmentRepositoryPort appointmentRepositoryPort) {
		this(appointmentRepositoryPort, null, null);
	}

	@Autowired
	CancelAppointmentService(
			AppointmentRepositoryPort appointmentRepositoryPort,
			CurrentUserResolver currentUserResolver,
			UserAuthorizationPolicy userAuthorizationPolicy
	) {
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.currentUserResolver = currentUserResolver;
		this.userAuthorizationPolicy = userAuthorizationPolicy;
	}

	@Override
	@Transactional
	public AppointmentResponse cancel(Long id) {
		Appointment appointment = appointmentRepositoryPort.findById(id)
				.orElseThrow(() -> new AppointmentNotFoundException("Appointment not found"));
		if (currentUserResolver != null) {
			userAuthorizationPolicy.ensureCanOperateAppointment(currentUserResolver.requireCurrentUser(), appointment);
		}

		return AppointmentResponse.from(appointmentRepositoryPort.save(appointment.cancel()));
	}
}
