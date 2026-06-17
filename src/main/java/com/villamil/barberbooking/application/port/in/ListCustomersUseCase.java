package com.villamil.barberbooking.application.port.in;

import java.util.List;

import com.villamil.barberbooking.application.dto.response.CustomerResponse;

public interface ListCustomersUseCase {

	List<CustomerResponse> list();
}
