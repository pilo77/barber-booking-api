package com.villamil.barberbooking.infrastructure.adapter.in.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.villamil.barberbooking.application.dto.command.GetBarberAvailabilityCommand;
import com.villamil.barberbooking.application.dto.response.BarberAvailabilityResponse;
import com.villamil.barberbooking.application.port.in.GetBarberAvailabilityUseCase;

import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1/barbers/{barberId}/availability")
public class BarberAvailabilityController {

	private final GetBarberAvailabilityUseCase getBarberAvailabilityUseCase;

	public BarberAvailabilityController(GetBarberAvailabilityUseCase getBarberAvailabilityUseCase) {
		this.getBarberAvailabilityUseCase = getBarberAvailabilityUseCase;
	}

	@GetMapping
	public ResponseEntity<BarberAvailabilityResponse> getAvailability(
			@PathVariable @Positive Long barberId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam @Positive Long serviceOfferingId
	) {
		return ResponseEntity.ok(getBarberAvailabilityUseCase.getAvailability(
				new GetBarberAvailabilityCommand(barberId, serviceOfferingId, date)
		));
	}
}
