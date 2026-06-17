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

import com.villamil.barberbooking.application.dto.response.CustomerResponse;
import com.villamil.barberbooking.application.port.in.CreateCustomerUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateCustomerUseCase;
import com.villamil.barberbooking.application.port.in.GetCustomerUseCase;
import com.villamil.barberbooking.application.port.in.ListCustomersUseCase;
import com.villamil.barberbooking.application.port.in.UpdateCustomerUseCase;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.CreateCustomerRequest;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.UpdateCustomerRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

	private final CreateCustomerUseCase createCustomerUseCase;
	private final GetCustomerUseCase getCustomerUseCase;
	private final ListCustomersUseCase listCustomersUseCase;
	private final UpdateCustomerUseCase updateCustomerUseCase;
	private final DeactivateCustomerUseCase deactivateCustomerUseCase;

	public CustomerController(
			CreateCustomerUseCase createCustomerUseCase,
			GetCustomerUseCase getCustomerUseCase,
			ListCustomersUseCase listCustomersUseCase,
			UpdateCustomerUseCase updateCustomerUseCase,
			DeactivateCustomerUseCase deactivateCustomerUseCase
	) {
		this.createCustomerUseCase = createCustomerUseCase;
		this.getCustomerUseCase = getCustomerUseCase;
		this.listCustomersUseCase = listCustomersUseCase;
		this.updateCustomerUseCase = updateCustomerUseCase;
		this.deactivateCustomerUseCase = deactivateCustomerUseCase;
	}

	@PostMapping
	public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
		CustomerResponse response = createCustomerUseCase.create(request.toCommand());
		return ResponseEntity
				.created(URI.create("/api/v1/customers/" + response.id()))
				.body(response);
	}

	@GetMapping
	public ResponseEntity<List<CustomerResponse>> list() {
		return ResponseEntity.ok(listCustomersUseCase.list());
	}

	@GetMapping("/{id}")
	public ResponseEntity<CustomerResponse> getById(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(getCustomerUseCase.getById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CustomerResponse> update(
			@PathVariable @Positive Long id,
			@Valid @RequestBody UpdateCustomerRequest request
	) {
		return ResponseEntity.ok(updateCustomerUseCase.update(id, request.toCommand()));
	}

	@PatchMapping("/{id}/deactivate")
	public ResponseEntity<CustomerResponse> deactivate(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(deactivateCustomerUseCase.deactivate(id));
	}
}
