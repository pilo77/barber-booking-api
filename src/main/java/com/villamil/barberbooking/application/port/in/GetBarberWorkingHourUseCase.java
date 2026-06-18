package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;

public interface GetBarberWorkingHourUseCase {

	BarberWorkingHourResponse getById(Long barberId, Long workingHourId);
}
