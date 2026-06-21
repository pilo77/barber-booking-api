package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.port.in.GetAppointmentUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.domain.exception.AppointmentNotFoundException;

@Service
class GetAppointmentService implements GetAppointmentUseCase {

	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final CurrentUserResolver currentUserResolver;
	private final UserAuthorizationPolicy userAuthorizationPolicy;

	GetAppointmentService(AppointmentRepositoryPort appointmentRepositoryPort) {
		this(appointmentRepositoryPort, null, null);
	}

	@Autowired
	GetAppointmentService(
			AppointmentRepositoryPort appointmentRepositoryPort,
			CurrentUserResolver currentUserResolver,
			UserAuthorizationPolicy userAuthorizationPolicy
	) {
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.currentUserResolver = currentUserResolver;
		this.userAuthorizationPolicy = userAuthorizationPolicy;
	}

	@Override
	@Transactional(readOnly = true)
	public AppointmentResponse getById(Long id) {
		var appointment = appointmentRepositoryPort.findById(id)
				.orElseThrow(() -> new AppointmentNotFoundException("Appointment not found"));
		if (currentUserResolver != null) {
			userAuthorizationPolicy.ensureCanOperateAppointment(currentUserResolver.requireCurrentUser(), appointment);
		}
		return AppointmentResponse.from(appointment);
	}
}
