package com.villamil.barberbooking.application.port.in;

import com.villamil.barberbooking.application.dto.command.GetBarberAvailabilityCommand;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;

public interface GetBarberAvailabilityUseCase {

	BarberAvailabilityResponse getAvailability(GetBarberAvailabilityCommand command);
}
