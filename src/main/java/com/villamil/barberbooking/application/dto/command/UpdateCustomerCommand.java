package com.villamil.barberbooking.application.dto.command;

public record UpdateCustomerCommand(
		String fullName,
		String phone,
		String email
) {
}
