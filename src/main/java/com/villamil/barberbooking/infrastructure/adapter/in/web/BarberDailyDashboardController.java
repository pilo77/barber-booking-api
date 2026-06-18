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

import com.villamil.barberbooking.application.dto.command.GetBarberDailyDashboardCommand;
import com.villamil.barberbooking.application.dto.response.BarberDailyDashboardResponse;
import com.villamil.barberbooking.application.port.in.GetBarberDailyDashboardUseCase;

import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1/barbers/{barberId}/daily-dashboard")
public class BarberDailyDashboardController {

	private final GetBarberDailyDashboardUseCase getBarberDailyDashboardUseCase;

	public BarberDailyDashboardController(GetBarberDailyDashboardUseCase getBarberDailyDashboardUseCase) {
		this.getBarberDailyDashboardUseCase = getBarberDailyDashboardUseCase;
	}

	@GetMapping
	public ResponseEntity<BarberDailyDashboardResponse> getDailyDashboard(
			@PathVariable @Positive Long barberId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return ResponseEntity.ok(getBarberDailyDashboardUseCase.getDailyDashboard(
				new GetBarberDailyDashboardCommand(barberId, date)
		));
	}
}
