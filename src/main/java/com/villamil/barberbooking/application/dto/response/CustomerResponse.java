package com.villamil.barberbooking.application.dto.response;

import java.time.Instant;

import com.villamil.barberbooking.domain.model.Customer;

public record CustomerResponse(
		Long id,
		String fullName,
		String phone,
		String email,
		Instant createdAt
) {

	public static CustomerResponse from(Customer customer) {
		return new CustomerResponse(
				customer.id(),
				customer.fullName(),
				customer.phone(),
				customer.email(),
				customer.createdAt()
		);
	}
}
