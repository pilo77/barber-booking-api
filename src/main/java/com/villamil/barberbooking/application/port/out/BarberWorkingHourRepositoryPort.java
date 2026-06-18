package com.villamil.barberbooking.application.port.out;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import com.villamil.barberbooking.domain.model.BarberWorkingHour;

public interface BarberWorkingHourRepositoryPort {

	BarberWorkingHour save(BarberWorkingHour workingHour);

	Optional<BarberWorkingHour> findByIdAndBarberId(Long id, Long barberId);

	List<BarberWorkingHour> findAllByBarberId(Long barberId);

	List<BarberWorkingHour> findActiveByBarberIdAndDay(Long barberId, DayOfWeek dayOfWeek);
}
