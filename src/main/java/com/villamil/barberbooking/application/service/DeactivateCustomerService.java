package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.port.in.DeactivateCustomerUseCase;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.model.Customer;

@Service
class DeactivateCustomerService implements DeactivateCustomerUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;

	DeactivateCustomerService(CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	@Transactional
	public CustomerResponse deactivate(Long id) {
		Customer customer = customerRepositoryPort.findById(id)
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

		return CustomerResponse.from(customerRepositoryPort.save(customer.deactivate()));
	}
}
