package com.villamil.barberbooking.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.villamil.barberbooking.application.dto.response.UserAccountResponse;
import com.villamil.barberbooking.application.port.in.ActivateUserAccountUseCase;
import com.villamil.barberbooking.application.port.in.CreateUserAccountUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateUserAccountUseCase;
import com.villamil.barberbooking.application.port.in.GetUserAccountUseCase;
import com.villamil.barberbooking.application.port.in.ListUserAccountsUseCase;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.CreateUserAccountRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1/user-accounts")
public class UserAccountController {

	private final CreateUserAccountUseCase createUserAccountUseCase;
	private final ListUserAccountsUseCase listUserAccountsUseCase;
	private final GetUserAccountUseCase getUserAccountUseCase;
	private final ActivateUserAccountUseCase activateUserAccountUseCase;
	private final DeactivateUserAccountUseCase deactivateUserAccountUseCase;

	public UserAccountController(
			CreateUserAccountUseCase createUserAccountUseCase,
			ListUserAccountsUseCase listUserAccountsUseCase,
			GetUserAccountUseCase getUserAccountUseCase,
			ActivateUserAccountUseCase activateUserAccountUseCase,
			DeactivateUserAccountUseCase deactivateUserAccountUseCase
	) {
		this.createUserAccountUseCase = createUserAccountUseCase;
		this.listUserAccountsUseCase = listUserAccountsUseCase;
		this.getUserAccountUseCase = getUserAccountUseCase;
		this.activateUserAccountUseCase = activateUserAccountUseCase;
		this.deactivateUserAccountUseCase = deactivateUserAccountUseCase;
	}

	@PostMapping
	public ResponseEntity<UserAccountResponse> create(@Valid @RequestBody CreateUserAccountRequest request) {
		UserAccountResponse response = createUserAccountUseCase.create(request.toCommand());
		return ResponseEntity
				.created(URI.create("/api/v1/user-accounts/" + response.id()))
				.body(response);
	}

	@GetMapping
	public ResponseEntity<List<UserAccountResponse>> list() {
		return ResponseEntity.ok(listUserAccountsUseCase.list());
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserAccountResponse> getById(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(getUserAccountUseCase.getById(id));
	}

	@PatchMapping("/{id}/activate")
	public ResponseEntity<UserAccountResponse> activate(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(activateUserAccountUseCase.activate(id));
	}

	@PatchMapping("/{id}/deactivate")
	public ResponseEntity<UserAccountResponse> deactivate(@PathVariable @Positive Long id) {
		return ResponseEntity.ok(deactivateUserAccountUseCase.deactivate(id));
	}
}
