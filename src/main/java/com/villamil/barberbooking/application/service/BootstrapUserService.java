package com.villamil.barberbooking.application.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.BootstrapUserCommand;
import com.villamil.barberbooking.application.dto.response.UserAccountResponse;
import com.villamil.barberbooking.application.port.in.BootstrapUserUseCase;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

@Service
class BootstrapUserService implements BootstrapUserUseCase {

	private final UserAccountRepositoryPort userAccountRepositoryPort;
	private final PasswordHasherPort passwordHasherPort;
	private final String bootstrapToken;

	BootstrapUserService(
			UserAccountRepositoryPort userAccountRepositoryPort,
			PasswordHasherPort passwordHasherPort,
			@Value("${app.bootstrap.token:}") String bootstrapToken
	) {
		this.userAccountRepositoryPort = userAccountRepositoryPort;
		this.passwordHasherPort = passwordHasherPort;
		this.bootstrapToken = bootstrapToken;
	}

	@Override
	@Transactional
	public UserAccountResponse bootstrap(BootstrapUserCommand command) {
		if (bootstrapToken == null || bootstrapToken.isBlank()) {
			throw new ForbiddenOperationException("Bootstrap token is not configured");
		}
		if (!bootstrapToken.equals(command.bootstrapToken())) {
			throw new ForbiddenOperationException("Invalid bootstrap token");
		}
		if (userAccountRepositoryPort.existsAny()) {
			throw new BusinessRuleException("Bootstrap is only allowed before creating the first user");
		}
		String email = normalizeEmail(command.email());
		UserAccount userAccount = UserAccount.create(
				1L,
				1L,
				email,
				passwordHasherPort.hash(command.password()),
				command.fullName(),
				null,
				Set.of(Role.COMPANY_OWNER)
		);
		return UserAccountResponse.from(userAccountRepositoryPort.save(userAccount));
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new BusinessRuleException("Email is required");
		}
		return email.strip().toLowerCase();
	}
}
