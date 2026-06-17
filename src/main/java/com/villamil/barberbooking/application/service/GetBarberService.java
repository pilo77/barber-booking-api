package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.BarberResponse;
import com.villamil.barberbooking.application.port.in.GetBarberUseCase;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;

@Service
class GetBarberService implements GetBarberUseCase {

	private final BarberRepositoryPort barberRepositoryPort;

	GetBarberService(BarberRepositoryPort barberRepositoryPort) {
		this.barberRepositoryPort = barberRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public BarberResponse getById(Long id) {
		return barberRepositoryPort.findById(id)
				.map(BarberResponse::from)
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));
	}
}
