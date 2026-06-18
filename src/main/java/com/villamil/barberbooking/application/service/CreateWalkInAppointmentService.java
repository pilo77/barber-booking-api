package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	CreateWalkInAppointmentService(
			AppointmentRepositoryPort appointmentRepositoryPort,
			AppointmentBookingPolicy appointmentBookingPolicy
	) {
		this.appointmentRepositoryPort = appointmentRepositoryPort;
		this.appointmentBookingPolicy = appointmentBookingPolicy;
	}

	@Override
	@Transactional
	public AppointmentResponse create(CreateWalkInAppointmentCommand command) {
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
