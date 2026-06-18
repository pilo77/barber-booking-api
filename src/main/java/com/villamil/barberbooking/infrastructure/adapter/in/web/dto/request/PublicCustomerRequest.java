package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import com.villamil.barberbooking.application.dto.command.PublicCustomerCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicCustomerRequest(
		@NotBlank(message = "Customer full name is required")
		@Size(max = 120, message = "Customer full name must be at most 120 characters")
		String fullName,

		@NotBlank(message = "Customer phone is required")
		@Size(max = 30, message = "Customer phone must be at most 30 characters")
		String phone,

		@Email(message = "Customer email must be valid")
		@Size(max = 120, message = "Customer email must be at most 120 characters")
		String email
) {

	public PublicCustomerCommand toCommand() {
		return new PublicCustomerCommand(fullName, phone, email);
	}
}
