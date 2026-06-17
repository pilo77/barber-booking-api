package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.UpdateCustomerCommand;
import com.villamil.barberbooking.application.dto.response.CustomerResponse;

public interface UpdateCustomerUseCase {

	CustomerResponse update(Long id, UpdateCustomerCommand command);
}
