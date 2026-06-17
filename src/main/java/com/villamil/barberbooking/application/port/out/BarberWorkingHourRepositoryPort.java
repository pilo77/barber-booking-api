package com.villamil.barberbooking.application.port.out;

import java.time.DayOfWeek;
import java.util.List;

import com.villamil.barberbooking.domain.model.BarberWorkingHour;

public interface BarberWorkingHourRepositoryPort {

	List<BarberWorkingHour> findActiveByBarberIdAndDay(Long barberId, DayOfWeek dayOfWeek);
}
