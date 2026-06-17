package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.response.CustomerResponse;

public interface GetCustomerUseCase {

	CustomerResponse getById(Long id);
}
