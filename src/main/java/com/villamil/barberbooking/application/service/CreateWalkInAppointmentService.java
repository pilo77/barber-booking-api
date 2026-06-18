package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.villamil.barberbooking.application.dto.command.CreateWalkInAppointmentCommand;
import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.port.in.CreateWalkInAppointmentUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.domain.model.Appointment;
import com.villamil.barberbooking.domain.valueobject.AppointmentSource;
import com.villamil.barberbooking.domain.valueobject.AppointmentStatus;

@Service
class CreateWalkInAppointmentService implements CreateWalkInAppointmentUseCase {

	private final AppointmentRepositoryPort appointmentRepositoryPort;
	private final AppointmentBookingPolicy appointmentBookingPolicy;
	private final CurrentUserResolver currentUserResolver;
	private final UserAuthorizationPolicy userAuthorizationPolicy;

	CreateWalkInAppointmentService(
			AppointmentRepositoryPort appointmentRepositoryPort,
			AppointmentBookingPolicy appointmentBookingPolicy
	) {
		this(appointmentRepositoryPort, appointmentBookingPolicy, null, null);
	}

	@Autowired
	CreateWalkInAppointmentService(
			AppointmentRepositoryPort appointmentRepositoryPort,
			AppointmentBookingPolicy appointmentBookingPolicy,
			CurrentUserResolver currentUserResolver,
			UserAuthorizationPolicy userAuthorizationPolicy
	) {
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.appointmentBookingPolicy = appointmentBookingPolicy;
		this.currentUserResolver = currentUserResolver;
		this.userAuthorizationPolicy = userAuthorizationPolicy;
	}

	@Override
	@Transactional
	public AppointmentResponse create(CreateWalkInAppointmentCommand command) {
		if (currentUserResolver != null) {
			userAuthorizationPolicy.ensureCanCreateAppointmentForBarber(
					currentUserResolver.requireCurrentUser(),
					command.barberId()
			);
		}
		AppointmentStatus initialStatus = command.startImmediately()
				? AppointmentStatus.IN_PROGRESS
				: AppointmentStatus.SCHEDULED;
		Appointment appointment = appointmentBookingPolicy.createValidatedAppointment(
				command.customerId(),
				command.barberId(),
				command.serviceOfferingId(),
				command.startAt(),
				AppointmentSource.WALK_IN,
				initialStatus
		);
		return AppointmentResponse.from(appointmentRepositoryPort.save(appointment));
	}
}
