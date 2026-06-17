package com.villamil.barberbooking.application.port.in;

import java.time.LocalDate;
import java.util.List;

import com.villamil.barberbooking.application.dto.response.AvailabilitySlotResponse;

public interface GetBarberAvailabilityUseCase {

	List<AvailabilitySlotResponse> getAvailability(Long barberId, LocalDate date, Long serviceId);
}
