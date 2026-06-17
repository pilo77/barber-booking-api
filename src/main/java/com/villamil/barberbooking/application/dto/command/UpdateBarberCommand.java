package com.villamil.barberbooking.application.dto.command;

public record UpdateBarberCommand(
		String fullName,
		String phone,
		String email
) {
}
