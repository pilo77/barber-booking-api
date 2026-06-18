package com.villamil.barberbooking.infrastructure.adapter.in.web;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.villamil.barberbooking.application.dto.response.AppointmentResponse;
import com.villamil.barberbooking.application.dto.response.BarberDailyScheduleResponse;
import com.villamil.barberbooking.application.port.in.BookAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.CancelAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.GetAppointmentUseCase;
import com.villamil.barberbooking.application.port.in.GetBarberDailyAppointmentsUseCase;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.BookAppointmentRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1")
public class AppointmentController {

	private final BookAppointmentUseCase bookAppointmentUseCase;
	private final GetAppointmentUseCase getAppointmentUseCase;
	private final GetBarberDailyAppointmentsUseCase getBarberDailyAppointmentsUseCase;
	private final CancelAppointmentUseCase cancelAppointmentUseCase;

	public AppointmentController(
			BookAppointmentUseCase bookAppointmentUseCase,
			GetAppointmentUseCase getAppointmentUseCase,
			GetBarberDailyAppointmentsUseCase getBarberDailyAppointmentsUseCase,
			CancelAppointmentUseCase cancelAppointmentUseCase
	) {
		this.bookAppointmentUseCase = bookAppointmentUseCase;
		this.getAppointmentUseCase = getAppointmentUseCase;
		this.getBarberDailyAppointmentsUseCase = getBarberDailyAppointmentsUseCase;
		this.cancelAppointmentUseCase = cancelAppointmentUseCase;
	}

	@PostMapping("/appointments")
	public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody BookAppointmentRequest request) {
		AppointmentResponse response = bookAppointmentUseCase.book(request.toCommand());
		return ResponseEntity
				.created(URI.create("/api/v1/appointments/" + response.id()))
				.body(response);
	}

	@GetMapping("/appointments/{id}")
	public ResponseEntity<AppointmentResponse> getById(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(getAppointmentUseCase.getById(id));
	}

	@GetMapping("/barbers/{barberId}/appointments")
	public ResponseEntity<BarberDailyScheduleResponse> getDailyAppointments(
			@PathVariable @Positive Long barberId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return ResponseEntity.ok(getBarberDailyAppointmentsUseCase.getDailyAppointments(barberId, date));
	}

	@PatchMapping("/appointments/{id}/cancel")
	public ResponseEntity<AppointmentResponse> cancel(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(cancelAppointmentUseCase.cancel(id));
	}
}
