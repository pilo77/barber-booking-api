package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.UpdateCustomerCommand;
import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.port.in.UpdateCustomerUseCase;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;
import com.villamil.barberbooking.domain.exception.CustomerNotFoundException;
import com.villamil.barberbooking.domain.model.Customer;

@Service
class UpdateCustomerService implements UpdateCustomerUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;
	private final CustomerUniquenessValidator customerUniquenessValidator;

	UpdateCustomerService(
			CustomerRepositoryPort customerRepositoryPort,
			CustomerUniquenessValidator customerUniquenessValidator
	) {
		this.customerRepositoryPort = customerRepositoryPort;
		this.customerUniquenessValidator = customerUniquenessValidator;
	}

	@Override
	@Transactional
	public CustomerResponse update(Long id, UpdateCustomerCommand command) {
		Customer currentCustomer = customerRepositoryPort.findById(id)
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
		Customer updatedCustomer = currentCustomer.update(command.fullName(), command.phone(), command.email());

		customerUniquenessValidator.ensureCanUpdate(updatedCustomer);

		return CustomerResponse.from(customerRepositoryPort.save(updatedCustomer));
	}
}
