package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.UpdateServiceOfferingCommand;
import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;

public interface UpdateServiceOfferingUseCase {

	ServiceOfferingResponse update(Long id, UpdateServiceOfferingCommand command);
}
