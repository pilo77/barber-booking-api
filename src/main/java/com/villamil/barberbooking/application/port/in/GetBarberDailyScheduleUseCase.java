package com.villamil.barberbooking.application.port.in;

import java.time.LocalDate;

import com.villamil.barberbooking.application.dto.response.BarberDailyScheduleResponse;

public interface GetBarberDailyScheduleUseCase {

	BarberDailyScheduleResponse getDailySchedule(Long barberId, LocalDate date);
}
