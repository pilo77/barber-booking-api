package com.villamil.barberbooking.application.dto.command;

import java.time.LocalDateTime;

public record CreatePublicAppointmentCommand(
		String companySlug,
		String branchSlug,
		Long serviceOfferingId,
		Long barberId,
		LocalDateTime startAt,
		PublicCustomerCommand customer
) {
}
