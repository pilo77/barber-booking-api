package com.villamil.barberbooking.application.dto.command;

public record PublicCustomerCommand(
		String fullName,
		String phone,
		String email
) {
}
