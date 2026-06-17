package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberAlreadyExistsException;
import com.villamil.barberbooking.domain.model.Barber;

@Component
class BarberUniquenessValidator {

	private final BarberRepositoryPort barberRepositoryPort;

	BarberUniquenessValidator(BarberRepositoryPort barberRepositoryPort) {
		this.barberRepositoryPort = barberRepositoryPort;
	}

	void ensureCanCreate(Barber barber) {
		if (barberRepositoryPort.existsByPhone(barber.phone())) {
			throw new BarberAlreadyExistsException("Barber phone already exists");
		}
		if (barber.email() != null && barberRepositoryPort.existsByEmail(barber.email())) {
			throw new BarberAlreadyExistsException("Barber email already exists");
		}
	}

	void ensureCanUpdate(Barber barber) {
		if (barberRepositoryPort.existsByPhoneAndIdNot(barber.phone(), barber.id())) {
			throw new BarberAlreadyExistsException("Barber phone already exists");
		}
		if (barber.email() != null && barberRepositoryPort.existsByEmailAndIdNot(barber.email(), barber.id())) {
			throw new BarberAlreadyExistsException("Barber email already exists");
		}
	}
}
