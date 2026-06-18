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

import com.villamil.barberbooking.application.dto.response.ServiceOfferingResponse;
import com.villamil.barberbooking.application.port.in.ActivateServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.in.CreateServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.in.GetServiceOfferingUseCase;
import com.villamil.barberbooking.application.port.in.ListServiceOfferingsUseCase;
import com.villamil.barberbooking.application.port.in.UpdateServiceOfferingUseCase;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.CreateServiceOfferingRequest;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.UpdateServiceOfferingRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1/services")
public class ServiceOfferingController {

	private final CreateServiceOfferingUseCase createServiceOfferingUseCase;
	private final GetServiceOfferingUseCase getServiceOfferingUseCase;
	private final ListServiceOfferingsUseCase listServiceOfferingsUseCase;
	private final UpdateServiceOfferingUseCase updateServiceOfferingUseCase;
	private final ActivateServiceOfferingUseCase activateServiceOfferingUseCase;
	private final DeactivateServiceOfferingUseCase deactivateServiceOfferingUseCase;

	public ServiceOfferingController(
			CreateServiceOfferingUseCase createServiceOfferingUseCase,
			GetServiceOfferingUseCase getServiceOfferingUseCase,
			ListServiceOfferingsUseCase listServiceOfferingsUseCase,
			UpdateServiceOfferingUseCase updateServiceOfferingUseCase,
			ActivateServiceOfferingUseCase activateServiceOfferingUseCase,
			DeactivateServiceOfferingUseCase deactivateServiceOfferingUseCase
	) {
		this.createServiceOfferingUseCase = createServiceOfferingUseCase;
		this.getServiceOfferingUseCase = getServiceOfferingUseCase;
		this.listServiceOfferingsUseCase = listServiceOfferingsUseCase;
		this.updateServiceOfferingUseCase = updateServiceOfferingUseCase;
		this.activateServiceOfferingUseCase = activateServiceOfferingUseCase;
		this.deactivateServiceOfferingUseCase = deactivateServiceOfferingUseCase;
	}

	@PostMapping
	public ResponseEntity<ServiceOfferingResponse> create(@Valid @RequestBody CreateServiceOfferingRequest request) {
		ServiceOfferingResponse response = createServiceOfferingUseCase.create(request.toCommand());
		return ResponseEntity
				.created(URI.create("/api/v1/services/" + response.id()))
				.body(response);
	}

	@GetMapping
	public ResponseEntity<List<ServiceOfferingResponse>> list() {
		return ResponseEntity.ok(listServiceOfferingsUseCase.list());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ServiceOfferingResponse> getById(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(getServiceOfferingUseCase.getById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ServiceOfferingResponse> update(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateServiceOfferingRequest request
	) {
		return ResponseEntity.ok(updateServiceOfferingUseCase.update(id, request.toCommand()));
	}

	@PatchMapping("/{id}/activate")
	public ResponseEntity<ServiceOfferingResponse> activate(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(activateServiceOfferingUseCase.activate(id));
	}

	@PatchMapping("/{id}/deactivate")
	public ResponseEntity<ServiceOfferingResponse> deactivate(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(deactivateServiceOfferingUseCase.deactivate(id));
	}
}
