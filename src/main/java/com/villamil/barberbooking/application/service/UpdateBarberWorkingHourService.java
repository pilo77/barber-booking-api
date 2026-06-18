package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.UpdateBarberWorkingHourCommand;
import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;
import com.villamil.barberbooking.application.port.in.UpdateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourNotFoundException;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;

@Service
class UpdateBarberWorkingHourService implements UpdateBarberWorkingHourUseCase {

	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;
	private final BarberWorkingHourBarberValidator barberValidator;
	private final BarberWorkingHourOverlapValidator overlapValidator;

	UpdateBarberWorkingHourService(
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
	public BarberWorkingHourResponse update(Long barberId, Long workingHourId, UpdateBarberWorkingHourCommand command) {
		barberValidator.ensureActive(barberId);
		BarberWorkingHour currentWorkingHour = barberWorkingHourRepositoryPort.findByIdAndBarberId(workingHourId, barberId)
				.orElseThrow(() -> new BarberWorkingHourNotFoundException("Working hour not found"));
		BarberWorkingHour updatedWorkingHour = currentWorkingHour.update(
				command.dayOfWeek(),
				command.startTime(),
				command.endTime()
		);

		if (updatedWorkingHour.active()) {
			overlapValidator.ensureNoActiveOverlap(updatedWorkingHour);
		}

		return BarberWorkingHourResponse.from(barberWorkingHourRepositoryPort.save(updatedWorkingHour));
	}
}
