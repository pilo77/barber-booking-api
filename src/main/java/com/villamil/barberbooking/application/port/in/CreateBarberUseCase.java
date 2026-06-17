package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.CreateBarberCommand;
import com.villamil.barberbooking.application.dto.response.BarberResponse;

public interface CreateBarberUseCase {

	BarberResponse create(CreateBarberCommand command);
}
