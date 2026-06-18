package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;
import com.villamil.barberbooking.application.port.in.GetBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourNotFoundException;

@Service
class GetBarberWorkingHourService implements GetBarberWorkingHourUseCase {

	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;
	private final BarberWorkingHourBarberValidator barberValidator;

	GetBarberWorkingHourService(
			BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort,
			BarberWorkingHourBarberValidator barberValidator
	) {
		this.barberWorkingHourRepositoryPort = barberWorkingHourRepositoryPort;
		this.barberValidator = barberValidator;
	}

	@Override
	@Transactional(readOnly = true)
	public BarberWorkingHourResponse getById(Long barberId, Long workingHourId) {
		barberValidator.ensureExists(barberId);
		return barberWorkingHourRepositoryPort.findByIdAndBarberId(workingHourId, barberId)
				.map(BarberWorkingHourResponse::from)
				.orElseThrow(() -> new BarberWorkingHourNotFoundException("Working hour not found"));
	}
}
