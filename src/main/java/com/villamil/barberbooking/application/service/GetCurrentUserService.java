package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.in.GetCurrentUserUseCase;

@Service
class GetCurrentUserService implements GetCurrentUserUseCase {

	private final CurrentUserResolver currentUserResolver;

	GetCurrentUserService(CurrentUserResolver currentUserResolver) {
		this.currentUserResolver = currentUserResolver;
	}

	@Override
	@Transactional(readOnly = true)
	public AuthenticatedUserResponse me() {
		return currentUserResolver.requireCurrentUser();
	}
}
