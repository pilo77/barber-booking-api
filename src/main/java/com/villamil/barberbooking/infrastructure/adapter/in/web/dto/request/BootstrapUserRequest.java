package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import com.villamil.barberbooking.application.dto.command.BootstrapUserCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BootstrapUserRequest(
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 120, message = "Email must be at most 120 characters")
		String email,

		@NotBlank(message = "Password is required")
		@Size(min = 12, max = 120, message = "Password must be between 12 and 120 characters")
		String password,

		@NotBlank(message = "Full name is required")
		@Size(max = 120, message = "Full name must be at most 120 characters")
		String fullName
) {

	public BootstrapUserCommand toCommand(String bootstrapToken) {
		return new BootstrapUserCommand(bootstrapToken, email, password, fullName);
	}
}
