package com.villamil.barberbooking.infrastructure.adapter.in.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.dto.response.LoginResponse;
import com.villamil.barberbooking.application.dto.response.UserAccountResponse;
import com.villamil.barberbooking.application.port.in.BootstrapUserUseCase;
import com.villamil.barberbooking.application.port.in.GetCurrentUserUseCase;
import com.villamil.barberbooking.application.port.in.LoginUseCase;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.BootstrapUserRequest;
import com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request.LoginRequest;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private static final String BOOTSTRAP_TOKEN_HEADER = "X-Bootstrap-Token";

	private final BootstrapUserUseCase bootstrapUserUseCase;
	private final LoginUseCase loginUseCase;
	private final GetCurrentUserUseCase getCurrentUserUseCase;

	public AuthController(
			BootstrapUserUseCase bootstrapUserUseCase,
			LoginUseCase loginUseCase,
			GetCurrentUserUseCase getCurrentUserUseCase
	) {
		this.bootstrapUserUseCase = bootstrapUserUseCase;
		this.loginUseCase = loginUseCase;
		this.getCurrentUserUseCase = getCurrentUserUseCase;
	}

	@PostMapping("/bootstrap")
	public ResponseEntity<UserAccountResponse> bootstrap(
			@RequestHeader(value = BOOTSTRAP_TOKEN_HEADER, required = false) String bootstrapToken,
			@Valid @RequestBody BootstrapUserRequest request
	) {
		UserAccountResponse response = bootstrapUserUseCase.bootstrap(request.toCommand(bootstrapToken));
		return ResponseEntity
				.created(URI.create("/api/v1/user-accounts/" + response.id()))
				.body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(loginUseCase.login(request.toCommand()));
	}

	@GetMapping("/me")
	public ResponseEntity<AuthenticatedUserResponse> me() {
		return ResponseEntity.ok(getCurrentUserUseCase.me());
	}
}
