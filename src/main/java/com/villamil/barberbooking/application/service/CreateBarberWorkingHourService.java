package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.CreateBarberWorkingHourCommand;
import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;
import com.villamil.barberbooking.application.port.in.CreateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;

@Service
class CreateBarberWorkingHourService implements CreateBarberWorkingHourUseCase {

	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;
	private final BarberWorkingHourBarberValidator barberValidator;
	private final BarberWorkingHourOverlapValidator overlapValidator;

	CreateBarberWorkingHourService(
			BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort,
			BarberWorkingHourBarberValidator barberValidator,
			BarberWorkingHourOverlapValidator overlapValidator
	) {
		this.barberWorkingHourRepositoryPort = barberWorkingHourRepositoryPort;
		this.barberValidator = barberValidator;
		this.overlapValidator = overlapValidator;
	}

	@Override
	@Transactional
	public BarberWorkingHourResponse create(Long barberId, CreateBarberWorkingHourCommand command) {
		barberValidator.ensureActive(barberId);
		BarberWorkingHour workingHour = BarberWorkingHour.create(
				barberId,
				command.dayOfWeek(),
				command.startTime(),
				command.endTime()
		);

		overlapValidator.ensureNoActiveOverlap(workingHour);

		return BarberWorkingHourResponse.from(barberWorkingHourRepositoryPort.save(workingHour));
	}
}
