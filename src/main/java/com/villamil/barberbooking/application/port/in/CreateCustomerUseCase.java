package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.CreateCustomerCommand;
import com.villamil.barberbooking.application.dto.response.CustomerResponse;

public interface CreateCustomerUseCase {

	CustomerResponse create(CreateCustomerCommand command);
}
