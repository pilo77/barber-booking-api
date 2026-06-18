package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;

public interface ActivateBarberWorkingHourUseCase {

	BarberWorkingHourResponse activate(Long barberId, Long workingHourId);
}
