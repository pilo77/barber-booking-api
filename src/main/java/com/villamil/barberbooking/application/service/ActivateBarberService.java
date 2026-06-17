package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.BarberResponse;
import com.villamil.barberbooking.application.port.in.ActivateBarberUseCase;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.model.Barber;

@Service
class ActivateBarberService implements ActivateBarberUseCase {

	private final BarberRepositoryPort barberRepositoryPort;

	ActivateBarberService(BarberRepositoryPort barberRepositoryPort) {
		this.barberRepositoryPort = barberRepositoryPort;
	}

	@Override
	@Transactional
	public BarberResponse activate(Long id) {
		Barber barber = barberRepositoryPort.findById(id)
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));

		return BarberResponse.from(barberRepositoryPort.save(barber.activate()));
	}
}
