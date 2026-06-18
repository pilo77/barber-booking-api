package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;

public interface DeactivateBarberWorkingHourUseCase {

	BarberWorkingHourResponse deactivate(Long barberId, Long workingHourId);
}
