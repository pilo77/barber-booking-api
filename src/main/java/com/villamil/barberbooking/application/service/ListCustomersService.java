package com.villamil.barberbooking.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.port.in.ListCustomersUseCase;
import com.villamil.barberbooking.application.port.out.CustomerRepositoryPort;

@Service
class ListCustomersService implements ListCustomersUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;

	ListCustomersService(CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CustomerResponse> list() {
		return customerRepositoryPort.findAll()
				.stream()
				.map(CustomerResponse::from)
				.toList();
	}
}
