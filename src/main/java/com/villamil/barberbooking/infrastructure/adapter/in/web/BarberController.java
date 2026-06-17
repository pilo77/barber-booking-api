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

import com.villamil.barberbooking.application.dto.response.BarberResponse;
import com.villamil.barberbooking.application.port.in.ActivateBarberUseCase;
import com.villamil.barberbooking.application.port.in.CreateBarberUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateBarberUseCase;
import com.villamil.barberbooking.application.port.in.GetBarberUseCase;
import com.villamil.barberbooking.application.port.in.ListBarbersUseCase;
import com.villamil.barberbooking.application.port.in.UpdateBarberUseCase;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.CreateBarberRequest;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.UpdateBarberRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1/barbers")
public class BarberController {

	private final CreateBarberUseCase createBarberUseCase;
	private final GetBarberUseCase getBarberUseCase;
	private final ListBarbersUseCase listBarbersUseCase;
	private final UpdateBarberUseCase updateBarberUseCase;
	private final ActivateBarberUseCase activateBarberUseCase;
	private final DeactivateBarberUseCase deactivateBarberUseCase;

	public BarberController(
			CreateBarberUseCase createBarberUseCase,
			GetBarberUseCase getBarberUseCase,
			ListBarbersUseCase listBarbersUseCase,
			UpdateBarberUseCase updateBarberUseCase,
			ActivateBarberUseCase activateBarberUseCase,
			DeactivateBarberUseCase deactivateBarberUseCase
	) {
		this.createBarberUseCase = createBarberUseCase;
		this.getBarberUseCase = getBarberUseCase;
		this.listBarbersUseCase = listBarbersUseCase;
		this.updateBarberUseCase = updateBarberUseCase;
		this.activateBarberUseCase = activateBarberUseCase;
		this.deactivateBarberUseCase = deactivateBarberUseCase;
	}

	@PostMapping
	public ResponseEntity<BarberResponse> create(@Valid @RequestBody CreateBarberRequest request) {
		BarberResponse response = createBarberUseCase.create(request.toCommand());
		return ResponseEntity
				.created(URI.create("/api/v1/barbers/" + response.id()))
				.body(response);
	}

	@GetMapping
	public ResponseEntity<List<BarberResponse>> list() {
		return ResponseEntity.ok(listBarbersUseCase.list());
	}

	@GetMapping("/{id}")
	public ResponseEntity<BarberResponse> getById(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(getBarberUseCase.getById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<BarberResponse> update(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateBarberRequest request
	) {
		return ResponseEntity.ok(updateBarberUseCase.update(id, request.toCommand()));
	}

	@PatchMapping("/{id}/activate")
	public ResponseEntity<BarberResponse> activate(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(activateBarberUseCase.activate(id));
	}

	@PatchMapping("/{id}/deactivate")
	public ResponseEntity<BarberResponse> deactivate(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(deactivateBarberUseCase.deactivate(id));
	}
}
