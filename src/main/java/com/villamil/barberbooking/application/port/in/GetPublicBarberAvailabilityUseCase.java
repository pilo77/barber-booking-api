package com.villamil.barberbooking.application.port.in;

import java.time.LocalDate;

import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;

public interface GetPublicBarberAvailabilityUseCase {

	BarberAvailabilityResponse getAvailability(
			String companySlug,
			String branchSlug,
			Long barberId,
			Long serviceOfferingId,
			LocalDate date
	);
}
