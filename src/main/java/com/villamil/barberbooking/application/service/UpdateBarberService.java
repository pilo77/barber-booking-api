package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.UpdateBarberCommand;
import com.villamil.barberbooking.application.dto.response.BarberResponse;
import com.villamil.barberbooking.application.port.in.UpdateBarberUseCase;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.model.Barber;

@Service
class UpdateBarberService implements UpdateBarberUseCase {

	private final BarberRepositoryPort barberRepositoryPort;
	private final BarberUniquenessValidator barberUniquenessValidator;

	UpdateBarberService(
			BarberRepositoryPort barberRepositoryPort,
			BarberUniquenessValidator barberUniquenessValidator
	) {
		this.barberRepositoryPort = barberRepositoryPort;
		this.barberUniquenessValidator = barberUniquenessValidator;
	}

	@Override
	@Transactional
	public BarberResponse update(Long id, UpdateBarberCommand command) {
		Barber currentBarber = barberRepositoryPort.findById(id)
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));
		Barber updatedBarber = currentBarber.update(command.fullName(), command.phone(), command.email());

		barberUniquenessValidator.ensureCanUpdate(updatedBarber);

		return BarberResponse.from(barberRepositoryPort.save(updatedBarber));
	}
}
