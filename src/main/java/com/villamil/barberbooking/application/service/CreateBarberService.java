package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.CreateBarberCommand;
import com.villamil.barberbooking.application.dto.response.BarberResponse;
import com.villamil.barberbooking.application.port.in.CreateBarberUseCase;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.model.Barber;

@Service
class CreateBarberService implements CreateBarberUseCase {

	private final BarberRepositoryPort barberRepositoryPort;
	private final BarberUniquenessValidator barberUniquenessValidator;

	CreateBarberService(
			BarberRepositoryPort barberRepositoryPort,
			BarberUniquenessValidator barberUniquenessValidator
	) {
		this.barberRepositoryPort = barberRepositoryPort;
		this.barberUniquenessValidator = barberUniquenessValidator;
	}

	@Override
	@Transactional
	public BarberResponse create(CreateBarberCommand command) {
		Barber barber = Barber.create(command.fullName(), command.phone(), command.email());

		barberUniquenessValidator.ensureCanCreate(barber);

		return BarberResponse.from(barberRepositoryPort.save(barber));
	}
}
