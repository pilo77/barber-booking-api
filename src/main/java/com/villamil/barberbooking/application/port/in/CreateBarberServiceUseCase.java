package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.CreateBarberServiceCommand;
import com.villamil.barberbooking.application.dto.response.BarberServiceResponse;

public interface CreateBarberServiceUseCase {

	BarberServiceResponse create(CreateBarberServiceCommand command);
}
