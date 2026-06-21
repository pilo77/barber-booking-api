package com.villamil.barberbooking.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.LoginCommand;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.dto.response.LoginResponse;
import com.villamil.barberbooking.application.port.in.LoginUseCase;
import com.villamil.barberbooking.application.port.out.JwtTokenPort;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.exception.AuthenticationFailedException;
import com.villamil.barberbooking.domain.model.UserAccount;

@Service
class LoginService implements LoginUseCase {

	private final UserAccountRepositoryPort userAccountRepositoryPort;
	private final PasswordHasherPort passwordHasherPort;
	private final JwtTokenPort jwtTokenPort;

	LoginService(
			UserAccountRepositoryPort userAccountRepositoryPort,
			PasswordHasherPort passwordHasherPort,
			JwtTokenPort jwtTokenPort
	) {
		this.userAccountRepositoryPort = userAccountRepositoryPort;
		this.passwordHasherPort = passwordHasherPort;
		this.jwtTokenPort = jwtTokenPort;
	}

	@Override
	@Transactional(readOnly = true)
	public LoginResponse login(LoginCommand command) {
		UserAccount userAccount = userAccountRepositoryPort.findByEmail(normalizeEmail(command.email()))
				.orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));
		if (!userAccount.active()) {
			throw new AuthenticationFailedException("Invalid email or password");
		}
		if (!passwordHasherPort.matches(command.password(), userAccount.passwordHash())) {
			throw new AuthenticationFailedException("Invalid email or password");
		}
		return new LoginResponse(
				jwtTokenPort.createAccessToken(userAccount),
				"Bearer",
				jwtTokenPort.expiresInSeconds(),
				AuthenticatedUserResponse.from(userAccount)
		);
	}

	private String normalizeEmail(String email) {
		if (email == null) {
			return "";
		}
		return email.strip().toLowerCase();
	}
}
