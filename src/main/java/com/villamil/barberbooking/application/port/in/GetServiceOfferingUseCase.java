package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;

public interface GetServiceOfferingUseCase {

	ServiceOfferingResponse getById(Long id);
}
