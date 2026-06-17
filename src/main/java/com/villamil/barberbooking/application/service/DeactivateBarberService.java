package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.BarberResponse;
import com.villamil.barberbooking.application.port.in.DeactivateBarberUseCase;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.model.Barber;

@Service
class DeactivateBarberService implements DeactivateBarberUseCase {

	private final BarberRepositoryPort barberRepositoryPort;

	DeactivateBarberService(BarberRepositoryPort barberRepositoryPort) {
		this.barberRepositoryPort = barberRepositoryPort;
	}

	@Override
	@Transactional
	public BarberResponse deactivate(Long id) {
		Barber barber = barberRepositoryPort.findById(id)
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));

		return BarberResponse.from(barberRepositoryPort.save(barber.deactivate()));
	}
}
