package com.villamil.barberbooking.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.villamil.barberbooking.application.dto.response.BarberWorkingHourResponse;
import com.villamil.barberbooking.application.port.in.ActivateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.in.CreateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.in.GetBarberWorkingHourUseCase;
import com.villamil.barberbooking.application.port.in.ListBarberWorkingHoursUseCase;
import com.villamil.barberbooking.application.port.in.UpdateBarberWorkingHourUseCase;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.CreateBarberWorkingHourRequest;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.UpdateBarberWorkingHourRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1/barbers/{barberId}/working-hours")
public class BarberWorkingHourController {

	private final CreateBarberWorkingHourUseCase createBarberWorkingHourUseCase;
	private final GetBarberWorkingHourUseCase getBarberWorkingHourUseCase;
	private final ListBarberWorkingHoursUseCase listBarberWorkingHoursUseCase;
	private final UpdateBarberWorkingHourUseCase updateBarberWorkingHourUseCase;
	private final ActivateBarberWorkingHourUseCase activateBarberWorkingHourUseCase;
	private final DeactivateBarberWorkingHourUseCase deactivateBarberWorkingHourUseCase;

	public BarberWorkingHourController(
			CreateBarberWorkingHourUseCase createBarberWorkingHourUseCase,
			GetBarberWorkingHourUseCase getBarberWorkingHourUseCase,
			ListBarberWorkingHoursUseCase listBarberWorkingHoursUseCase,
			UpdateBarberWorkingHourUseCase updateBarberWorkingHourUseCase,
			ActivateBarberWorkingHourUseCase activateBarberWorkingHourUseCase,
			DeactivateBarberWorkingHourUseCase deactivateBarberWorkingHourUseCase
	) {
		this.createBarberWorkingHourUseCase = createBarberWorkingHourUseCase;
		this.getBarberWorkingHourUseCase = getBarberWorkingHourUseCase;
		this.listBarberWorkingHoursUseCase = listBarberWorkingHoursUseCase;
		this.updateBarberWorkingHourUseCase = updateBarberWorkingHourUseCase;
		this.activateBarberWorkingHourUseCase = activateBarberWorkingHourUseCase;
		this.deactivateBarberWorkingHourUseCase = deactivateBarberWorkingHourUseCase;
	}

	@PostMapping
	public ResponseEntity<BarberWorkingHourResponse> create(
			@PathVariable @Positive Long barberId,
			@Valid @RequestBody CreateBarberWorkingHourRequest request
	) {
		BarberWorkingHourResponse response = createBarberWorkingHourUseCase.create(barberId, request.toCommand());
		return ResponseEntity
				.created(URI.create("/api/v1/barbers/" + barberId + "/working-hours/" + response.id()))
				.body(response);
	}

	@GetMapping
	public ResponseEntity<List<BarberWorkingHourResponse>> list(@PathVariable @Positive Long barberId) {
		return ResponseEntity.ok(listBarberWorkingHoursUseCase.list(barberId));
	}

	@GetMapping("/{workingHourId}")
	public ResponseEntity<BarberWorkingHourResponse> getById(
			@PathVariable @Positive Long barberId,
			@PathVariable @Positive Long workingHourId
	) {
		return ResponseEntity.ok(getBarberWorkingHourUseCase.getById(barberId, workingHourId));
	}

	@PutMapping("/{workingHourId}")
	public ResponseEntity<BarberWorkingHourResponse> update(
			@PathVariable @Positive Long barberId,
			@PathVariable @Positive Long workingHourId,
			@Valid @RequestBody UpdateBarberWorkingHourRequest request
	) {
		return ResponseEntity.ok(updateBarberWorkingHourUseCase.update(barberId, workingHourId, request.toCommand()));
	}

	@PatchMapping("/{workingHourId}/activate")
	public ResponseEntity<BarberWorkingHourResponse> activate(
			@PathVariable @Positive Long barberId,
			@PathVariable @Positive Long workingHourId
	) {
		return ResponseEntity.ok(activateBarberWorkingHourUseCase.activate(barberId, workingHourId));
	}

	@PatchMapping("/{workingHourId}/deactivate")
	public ResponseEntity<BarberWorkingHourResponse> deactivate(
			@PathVariable @Positive Long barberId,
			@PathVariable @Positive Long workingHourId
	) {
		return ResponseEntity.ok(deactivateBarberWorkingHourUseCase.deactivate(barberId, workingHourId));
	}
}
