package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.CurrentUserProvider;
import com.villamil.barberbooking.domain.exception.AuthenticationFailedException;

@Component
class CurrentUserResolver {

	private final CurrentUserProvider currentUserProvider;

	CurrentUserResolver(CurrentUserProvider currentUserProvider) {
		this.currentUserProvider = currentUserProvider;
	}

	AuthenticatedUserResponse requireCurrentUser() {
		return currentUserProvider.currentUser()
				.orElseThrow(() -> new AuthenticationFailedException("Authentication is required"));
	}
}
