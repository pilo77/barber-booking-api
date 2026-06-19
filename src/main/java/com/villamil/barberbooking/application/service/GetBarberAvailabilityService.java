package com.villamil.barberbooking.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.GetBarberAvailabilityCommand;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.port.in.GetBarberAvailabilityUseCase;
import com.villamil.barberbooking.application.port.out.AppointmentRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.application.port.out.ServiceOfferingRepositoryPort;

@Service
class GetBarberAvailabilityService implements GetBarberAvailabilityUseCase {

	private final BarberAvailabilityCalculator barberAvailabilityCalculator;
	private final CurrentUserResolver currentUserResolver;
	private final UserAuthorizationPolicy userAuthorizationPolicy;

	@Autowired
	GetBarberAvailabilityService(
			BarberAvailabilityCalculator barberAvailabilityCalculator,
			CurrentUserResolver currentUserResolver,
			UserAuthorizationPolicy userAuthorizationPolicy
	) {
		this.barberAvailabilityCalculator = barberAvailabilityCalculator;
		this.currentUserResolver = currentUserResolver;
		this.userAuthorizationPolicy = userAuthorizationPolicy;
	}

	GetBarberAvailabilityService(
			BarberRepositoryPort barberRepositoryPort,
			ServiceOfferingRepositoryPort serviceOfferingRepositoryPort,
			BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort,
			AppointmentRepositoryPort appointmentRepositoryPort,
			int slotStepMinutes
	) {
		this(
				new BarberAvailabilityCalculator(
						barberRepositoryPort,
						serviceOfferingRepositoryPort,
						barberWorkingHourRepositoryPort,
						appointmentRepositoryPort,
						slotStepMinutes
				),
				null,
				null
		);
	}

	@Override
	@Transactional(readOnly = true)
	public BarberAvailabilityResponse getAvailability(GetBarberAvailabilityCommand command) {
		if (currentUserResolver != null) {
			userAuthorizationPolicy.ensureCanAccessBarberSchedule(
					currentUserResolver.requireCurrentUser(),
					command.barberId()
			);
		}
		return barberAvailabilityCalculator.calculate(command);
	}
}
