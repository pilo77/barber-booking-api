package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;

public interface ActivateServiceOfferingUseCase {

	ServiceOfferingResponse activate(Long id);
}
