package com.villamil.barberbooking.application.dto.command;

public record CreateBarberCommand(
		String fullName,
		String phone
) {
}
