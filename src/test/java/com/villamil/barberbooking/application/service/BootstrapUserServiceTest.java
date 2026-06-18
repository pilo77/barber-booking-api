package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.BootstrapUserCommand;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

@ExtendWith(MockitoExtension.class)
class BootstrapUserServiceTest {

	@Mock
	private UserAccountRepositoryPort userAccountRepositoryPort;

	@Mock
	private PasswordHasherPort passwordHasherPort;

	@Test
	void bootstrapCreatesFirstCompanyOwnerWithHashedPassword() {
		BootstrapUserService service = new BootstrapUserService(userAccountRepositoryPort, passwordHasherPort, "token-123");
		when(userAccountRepositoryPort.existsAny()).thenReturn(false);
		when(passwordHasherPort.hash("StrongPassword123!")).thenReturn("$2a$hash");
		when(userAccountRepositoryPort.save(any(UserAccount.class))).thenAnswer(invocation -> {
			UserAccount user = invocation.getArgument(0);
			return new UserAccount(
					1L,
					user.companyId(),
					user.branchId(),
					user.email(),
					user.passwordHash(),
					user.fullName(),
					user.phone(),
					user.active(),
					Instant.parse("2026-06-18T12:00:00Z"),
					Instant.parse("2026-06-18T12:00:00Z"),
					user.roles()
			);
		});

		service.bootstrap(new BootstrapUserCommand("token-123", "OWNER@EXAMPLE.COM", "StrongPassword123!", "Owner User"));

		ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
		verify(userAccountRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().email()).isEqualTo("owner@example.com");
		assertThat(captor.getValue().passwordHash()).isEqualTo("$2a$hash");
		assertThat(captor.getValue().passwordHash()).isNotEqualTo("StrongPassword123!");
		assertThat(captor.getValue().roles()).isEqualTo(Set.of(Role.COMPANY_OWNER));
		assertThat(captor.getValue().companyId()).isEqualTo(1L);
		assertThat(captor.getValue().branchId()).isEqualTo(1L);
	}

	@Test
	void bootstrapFailsWithInvalidToken() {
		BootstrapUserService service = new BootstrapUserService(userAccountRepositoryPort, passwordHasherPort, "token-123");

		assertThatThrownBy(() -> service.bootstrap(new BootstrapUserCommand("bad", "owner@example.com", "StrongPassword123!", "Owner")))
				.isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	void bootstrapFailsWhenUserAlreadyExists() {
		BootstrapUserService service = new BootstrapUserService(userAccountRepositoryPort, passwordHasherPort, "token-123");
		when(userAccountRepositoryPort.existsAny()).thenReturn(true);

		assertThatThrownBy(() -> service.bootstrap(new BootstrapUserCommand("token-123", "owner@example.com", "StrongPassword123!", "Owner")))
				.isInstanceOf(BusinessRuleException.class);
	}
}
