package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;
import com.villamil.barberbooking.application.port.in.ActivateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourNotFoundException;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;

@Service
class ActivateBarberWorkingHourService implements ActivateBarberWorkingHourUseCase {

	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;
	private final BarberWorkingHourBarberValidator barberValidator;
	private final BarberWorkingHourOverlapValidator overlapValidator;

	ActivateBarberWorkingHourService(
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
	public BarberWorkingHourResponse activate(Long barberId, Long workingHourId) {
		barberValidator.ensureActive(barberId);
		BarberWorkingHour currentWorkingHour = barberWorkingHourRepositoryPort.findByIdAndBarberId(workingHourId, barberId)
				.orElseThrow(() -> new BarberWorkingHourNotFoundException("Working hour not found"));
		BarberWorkingHour activatedWorkingHour = currentWorkingHour.activate();

		overlapValidator.ensureNoActiveOverlap(activatedWorkingHour);

		return BarberWorkingHourResponse.from(barberWorkingHourRepositoryPort.save(activatedWorkingHour));
	}
}
