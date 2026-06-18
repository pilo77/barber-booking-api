package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.CreateBarberWorkingHourCommand;
import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;

public interface CreateBarberWorkingHourUseCase {

	BarberWorkingHourResponse create(Long barberId, CreateBarberWorkingHourCommand command);
}
