package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import com.villamil.barberbooking.application.dto.command.UpdateCustomerCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
		@NotBlank(message = "Full name is required")
		@Size(max = 120, message = "Full name must be at most 120 characters")
		String fullName,

		@NotBlank(message = "Phone is required")
		@Size(max = 30, message = "Phone must be at most 30 characters")
		String phone,

		@Email(message = "Email must be valid")
		@Size(max = 120, message = "Email must be at most 120 characters")
		String email
) {

	public UpdateCustomerCommand toCommand() {
		return new UpdateCustomerCommand(fullName, phone, email);
	}
}
