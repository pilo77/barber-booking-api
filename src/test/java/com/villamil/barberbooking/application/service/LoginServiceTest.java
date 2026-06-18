package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.LoginCommand;
import com.villamil.barberbooking.application.port.out.JwtTokenPort;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.exception.AuthenticationFailedException;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

	@Mock
	private UserAccountRepositoryPort userAccountRepositoryPort;

	@Mock
	private PasswordHasherPort passwordHasherPort;

	@Mock
	private JwtTokenPort jwtTokenPort;

	@Test
	void loginReturnsJwtWhenCredentialsAreValid() {
		UserAccount user = activeUser();
		LoginService service = new LoginService(userAccountRepositoryPort, passwordHasherPort, jwtTokenPort);
		when(userAccountRepositoryPort.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
		when(passwordHasherPort.matches("StrongPassword123!", "$2a$hash")).thenReturn(true);
		when(jwtTokenPort.createAccessToken(user)).thenReturn("jwt-token");
		when(jwtTokenPort.expiresInSeconds()).thenReturn(3600L);

		var response = service.login(new LoginCommand("OWNER@EXAMPLE.COM", "StrongPassword123!"));

		assertThat(response.accessToken()).isEqualTo("jwt-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600L);
		assertThat(response.user().roles()).contains(Role.COMPANY_OWNER);
	}

	@Test
	void loginFailsWhenPasswordIsInvalid() {
		UserAccount user = activeUser();
		LoginService service = new LoginService(userAccountRepositoryPort, passwordHasherPort, jwtTokenPort);
		when(userAccountRepositoryPort.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
		when(passwordHasherPort.matches("bad", "$2a$hash")).thenReturn(false);

		assertThatThrownBy(() -> service.login(new LoginCommand("owner@example.com", "bad")))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	@Test
	void loginFailsWhenUserIsInactive() {
		UserAccount inactive = activeUser().deactivate();
		LoginService service = new LoginService(userAccountRepositoryPort, passwordHasherPort, jwtTokenPort);
		when(userAccountRepositoryPort.findByEmail("owner@example.com")).thenReturn(Optional.of(inactive));

		assertThatThrownBy(() -> service.login(new LoginCommand("owner@example.com", "StrongPassword123!")))
				.isInstanceOf(AuthenticationFailedException.class);
	}

	private UserAccount activeUser() {
		return new UserAccount(
				1L,
				1L,
				1L,
				"owner@example.com",
				"$2a$hash",
				"Owner User",
				null,
				true,
				Instant.parse("2026-06-18T12:00:00Z"),
				Instant.parse("2026-06-18T12:00:00Z"),
				Set.of(Role.COMPANY_OWNER)
		);
	}
}
