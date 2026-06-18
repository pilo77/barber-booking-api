package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberNotFoundException;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.model.Barber;

@Component
class BarberWorkingHourBarberValidator {

	private final BarberRepositoryPort barberRepositoryPort;

	BarberWorkingHourBarberValidator(BarberRepositoryPort barberRepositoryPort) {
		this.barberRepositoryPort = barberRepositoryPort;
	}

	void ensureExists(Long barberId) {
		find(barberId);
	}

	void ensureActive(Long barberId) {
		Barber barber = find(barberId);
		if (!barber.active()) {
			throw new BusinessRuleException("Barber must be active");
		}
	}

	private Barber find(Long barberId) {
		return barberRepositoryPort.findById(barberId)
				.orElseThrow(() -> new BarberNotFoundException("Barber not found"));
	}
}
