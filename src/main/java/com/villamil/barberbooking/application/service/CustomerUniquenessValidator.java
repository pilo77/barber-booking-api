package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.domain.exception.CustomerAlreadyExistsException;
import com.villamil.barberbooking.domain.model.Customer;

@Component
class CustomerUniquenessValidator {

	private final CustomerRepositoryPort customerRepositoryPort;

	CustomerUniquenessValidator(CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	void ensureCanCreate(Customer customer) {
		if (customerRepositoryPort.existsByPhone(customer.phone())) {
			throw new CustomerAlreadyExistsException("Customer phone already exists");
		}
		if (customer.email() != null && customerRepositoryPort.existsByEmail(customer.email())) {
			throw new CustomerAlreadyExistsException("Customer email already exists");
		}
	}

	void ensureCanUpdate(Customer customer) {
		if (customerRepositoryPort.existsByPhoneAndIdNot(customer.phone(), customer.id())) {
			throw new CustomerAlreadyExistsException("Customer phone already exists");
		}
		if (customer.email() != null && customerRepositoryPort.existsByEmailAndIdNot(customer.email(), customer.id())) {
			throw new CustomerAlreadyExistsException("Customer email already exists");
		}
	}
}
