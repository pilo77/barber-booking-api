package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import com.villamil.barberbooking.application.dto.command.LoginCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 120, message = "Email must be at most 120 characters")
		String email,

		@NotBlank(message = "Password is required")
		@Size(max = 120, message = "Password must be at most 120 characters")
		String password
) {

	public LoginCommand toCommand() {
		return new LoginCommand(email, password);
	}
}
