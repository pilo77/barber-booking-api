package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.CreateCustomerCommand;
import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.port.in.CreateCustomerUseCase;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.domain.model.Customer;

@Service
class CreateCustomerService implements CreateCustomerUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;
	private final CustomerUniquenessValidator customerUniquenessValidator;

	CreateCustomerService(
			CustomerRepositoryPort customerRepositoryPort,
			CustomerUniquenessValidator customerUniquenessValidator
	) {
		this.customerRepositoryPort = customerRepositoryPort;
		this.customerUniquenessValidator = customerUniquenessValidator;
	}

	@Override
	@Transactional
	public CustomerResponse create(CreateCustomerCommand command) {
		Customer customer = Customer.create(command.fullName(), command.phone(), command.email());

		customerUniquenessValidator.ensureCanCreate(customer);

		return CustomerResponse.from(customerRepositoryPort.save(customer));
	}
}
