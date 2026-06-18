package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.UpdateBarberWorkingHourCommand;
import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;

public interface UpdateBarberWorkingHourUseCase {

	BarberWorkingHourResponse update(Long barberId, Long workingHourId, UpdateBarberWorkingHourCommand command);
}
