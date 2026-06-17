package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.port.in.GetCustomerUseCase;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;

@Service
class GetCustomerService implements GetCustomerUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;

	GetCustomerService(CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerResponse getById(Long id) {
		return customerRepositoryPort.findById(id)
				.map(CustomerResponse::from)
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
	}
}
