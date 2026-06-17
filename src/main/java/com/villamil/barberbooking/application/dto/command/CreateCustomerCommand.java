package com.villamil.barberbooking.application.dto.command;

public record CreateCustomerCommand(
		String fullName,
		String phone,
		String email
) {
}
