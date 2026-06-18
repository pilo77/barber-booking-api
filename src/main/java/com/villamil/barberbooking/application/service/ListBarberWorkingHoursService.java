package com.villamil.barberbooking.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;
import com.villamil.barberbooking.application.port.in.ListBarberWorkingHoursUseCase;
import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;

@Service
class ListBarberWorkingHoursService implements ListBarberWorkingHoursUseCase {

	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;
	private final BarberWorkingHourBarberValidator barberValidator;

	ListBarberWorkingHoursService(
			BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort,
			BarberWorkingHourBarberValidator barberValidator
	) {
		this.barberWorkingHourRepositoryPort = barberWorkingHourRepositoryPort;
		this.barberValidator = barberValidator;
	}

	@Override
	@Transactional(readOnly = true)
	public List<BarberWorkingHourResponse> list(Long barberId) {
		barberValidator.ensureExists(barberId);
		return barberWorkingHourRepositoryPort.findAllByBarberId(barberId)
				.stream()
				.map(BarberWorkingHourResponse::from)
				.toList();
	}
}
