package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.UpdateBarberCommand;
import com.villamil.barberbooking.application.dto.response.BarberResponse;

public interface UpdateBarberUseCase {

	BarberResponse update(Long id, UpdateBarberCommand command);
}
