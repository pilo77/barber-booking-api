package com.villamil.barberbooking.application.dto.command;

public record BootstrapUserCommand(
		String bootstrapToken,
		String email,
		String password,
		String fullName
) {
}
