package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import java.util.Set;

import com.villamil.barberbooking.application.dto.command.CreateUserAccountCommand;
import com.villamil.barberbooking.domain.model.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateUserAccountRequest(
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 120, message = "Email must be at most 120 characters")
		String email,

		@NotBlank(message = "Password is required")
		@Size(min = 12, max = 120, message = "Password must be between 12 and 120 characters")
		String password,

		@NotBlank(message = "Full name is required")
		@Size(max = 120, message = "Full name must be at most 120 characters")
		String fullName,

		@Size(max = 30, message = "Phone must be at most 30 characters")
		String phone,

		@NotEmpty(message = "At least one role is required")
		Set<Role> roles
) {

	public CreateUserAccountCommand toCommand() {
		return new CreateUserAccountCommand(email, password, fullName, phone, roles);
	}
}
