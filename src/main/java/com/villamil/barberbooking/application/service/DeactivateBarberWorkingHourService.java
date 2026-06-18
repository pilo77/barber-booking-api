package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;
import com.villamil.barberbooking.application.port.in.DeactivateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourNotFoundException;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;

@Service
class DeactivateBarberWorkingHourService implements DeactivateBarberWorkingHourUseCase {

	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;
	private final BarberWorkingHourBarberValidator barberValidator;

	DeactivateBarberWorkingHourService(
			BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort,
			BarberWorkingHourBarberValidator barberValidator
	) {
		this.barberWorkingHourRepositoryPort = barberWorkingHourRepositoryPort;
		this.barberValidator = barberValidator;
	}

	@Override
	@Transactional
	public BarberWorkingHourResponse deactivate(Long barberId, Long workingHourId) {
		barberValidator.ensureExists(barberId);
		BarberWorkingHour currentWorkingHour = barberWorkingHourRepositoryPort.findByIdAndBarberId(workingHourId, barberId)
				.orElseThrow(() -> new BarberWorkingHourNotFoundException("Working hour not found"));

		return BarberWorkingHourResponse.from(barberWorkingHourRepositoryPort.save(currentWorkingHour.deactivate()));
	}
}
