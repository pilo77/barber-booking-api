package com.villamil.barberbooking.application.service;

import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

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
	private final CurrentUserResolver currentUserResolver;
	private final UserAuthorizationPolicy userAuthorizationPolicy;

	AppointmentLifecycleService(AppointmentRepositoryPort appointmentRepositoryPort) {
		this(appointmentRepositoryPort, null, null);
	}

	@Autowired
	AppointmentLifecycleService(
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
		if (currentUserResolver != null) {
			userAuthorizationPolicy.ensureCanOperateAppointment(currentUserResolver.requireCurrentUser(), appointment);
		}
		return AppointmentResponse.from(appointmentRepositoryPort.save(transition.apply(appointment)));
	}
}
