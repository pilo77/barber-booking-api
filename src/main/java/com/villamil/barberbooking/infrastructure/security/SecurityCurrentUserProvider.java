package com.villamil.barberbooking.infrastructure.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.CurrentUserProvider;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

	@Override
	public Optional<AuthenticatedUserResponse> currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}
		if (authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
			return Optional.of(principal.user());
		}
		return Optional.empty();
	}
}
