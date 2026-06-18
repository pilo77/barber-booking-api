package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.CreateServiceOfferingCommand;
import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;

public interface CreateServiceOfferingUseCase {

	ServiceOfferingResponse create(CreateServiceOfferingCommand command);
}
