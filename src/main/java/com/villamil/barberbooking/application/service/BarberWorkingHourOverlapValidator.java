package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.BarberWorkingHourRepositoryPort;
import com.villamil.barberbooking.domain.exception.BarberWorkingHourOverlapException;
import com.villamil.barberbooking.domain.model.BarberWorkingHour;

@Component
class BarberWorkingHourOverlapValidator {

	private final BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort;

	BarberWorkingHourOverlapValidator(BarberWorkingHourRepositoryPort barberWorkingHourRepositoryPort) {
		this.barberWorkingHourRepositoryPort = barberWorkingHourRepositoryPort;
	}

	void ensureNoActiveOverlap(BarberWorkingHour candidate) {
		boolean overlaps = barberWorkingHourRepositoryPort
				.findActiveByBarberIdAndDay(candidate.barberId(), candidate.dayOfWeek())
				.stream()
				.filter(existing -> !existing.id().equals(candidate.id()))
				.anyMatch(existing -> existing.range().overlaps(candidate.range()));

		if (overlaps) {
			throw new BarberWorkingHourOverlapException("Working hour overlaps with an active working hour");
		}
	}
}
