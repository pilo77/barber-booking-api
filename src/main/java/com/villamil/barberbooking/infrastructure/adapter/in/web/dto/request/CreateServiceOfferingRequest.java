package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import java.math.BigDecimal;

import com.villamil.barberbooking.application.dto.command.CreateServiceOfferingCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateServiceOfferingRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 120, message = "Name must be at most 120 characters")
		String name,

		@Size(max = 255, message = "Description must be at most 255 characters")
		String description,

		@Positive(message = "Duration minutes must be positive")
		int durationMinutes,

		@NotNull(message = "Price is required")
		@DecimalMin(value = "0.00", message = "Price must be zero or positive")
		@Digits(integer = 10, fraction = 2, message = "Price must have up to 10 integer digits and 2 decimals")
		BigDecimal price
) {

	public CreateServiceOfferingCommand toCommand() {
		return new CreateServiceOfferingCommand(name, description, durationMinutes, price);
	}
}
