package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import java.time.LocalDateTime;

import com.villamil.barberbooking.application.dto.command.CreatePublicAppointmentCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;

public record CreatePublicAppointmentRequest(
		@NotNull(message = "Service offering id is required")
		@Positive(message = "Service offering id must be positive")
		Long serviceOfferingId,

		@NotNull(message = "Barber id is required")
		@Positive(message = "Barber id must be positive")
		Long barberId,

		@NotNull(message = "Appointment start date is required")
		@Future(message = "Appointment start date must be in the future")
		LocalDateTime startAt,

		@NotNull(message = "Customer is required")
		@Valid
		PublicCustomerRequest customer,

		@Null(message = "Company id is resolved from the public URL")
		Long companyId,

		@Null(message = "Branch id is resolved from the public URL")
		Long branchId,

		@Null(message = "Customer id is not accepted for public booking")
		Long customerId,

		@Null(message = "Appointment end date is calculated by the server")
		LocalDateTime endAt
) {

	public CreatePublicAppointmentCommand toCommand(String companySlug, String branchSlug) {
		return new CreatePublicAppointmentCommand(
				companySlug,
				branchSlug,
				serviceOfferingId,
				barberId,
				startAt,
				customer.toCommand()
		);
	}
}
