package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.villamil.barberbooking.application.dto.command.CreateUserAccountCommand;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.BranchRepositoryPort;
import com.villamil.barberbooking.application.port.out.CurrentUserProvider;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

	@Mock
	private UserAccountRepositoryPort userAccountRepositoryPort;

	@Mock
	private BranchRepositoryPort branchRepositoryPort;

	@Mock
	private BarberRepositoryPort barberRepositoryPort;

	@Mock
	private PasswordHasherPort passwordHasherPort;

	@Mock
	private CurrentUserProvider currentUserProvider;

	@Test
	void companyOwnerCannotCreatePlatformOwner() {
		UserAccountService service = service(companyOwner());

		assertThatThrownBy(() -> service.create(command(Set.of(Role.PLATFORM_OWNER), null, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Requested role is not assignable from this endpoint");
	}

	@Test
	void companyOwnerCannotCreateUserInBranchFromAnotherCompany() {
		UserAccountService service = service(companyOwner());
		when(branchRepositoryPort.existsByIdAndCompanyId(2L, 1L)).thenReturn(false);

		assertThatThrownBy(() -> service.create(command(Set.of(Role.RECEPTIONIST), 2L, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Branch does not belong to current company");
	}

	@Test
	void branchManagerCannotCreateCompanyOwner() {
		UserAccountService service = service(branchManager());

		assertThatThrownBy(() -> service.create(command(Set.of(Role.COMPANY_OWNER), null, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Requested role is not assignable from this endpoint");
	}

	@Test
	void barberUserRequiresBarberId() {
		UserAccountService service = service(companyOwner());
		when(branchRepositoryPort.existsByIdAndCompanyId(1L, 1L)).thenReturn(true);

		assertThatThrownBy(() -> service.create(command(Set.of(Role.BARBER), null, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("BARBER users require barberId");
	}

	@Test
	void companyOwnerCanCreateBarberUserLinkedToBarberInSameTenant() {
		UserAccountService service = service(companyOwner());
		when(branchRepositoryPort.existsByIdAndCompanyId(1L, 1L)).thenReturn(true);
		when(barberRepositoryPort.existsByIdAndCompanyIdAndBranchId(5L, 1L, 1L)).thenReturn(true);
		when(userAccountRepositoryPort.existsByEmail("barber@example.com")).thenReturn(false);
		when(userAccountRepositoryPort.existsByBarberId(5L)).thenReturn(false);
		when(passwordHasherPort.hash("StrongPassword123!")).thenReturn("$2a$hash");
		when(userAccountRepositoryPort.save(any(UserAccount.class))).thenAnswer(invocation -> {
			UserAccount user = invocation.getArgument(0);
			return new UserAccount(
					10L,
					user.companyId(),
					user.branchId(),
					user.email(),
					user.passwordHash(),
					user.fullName(),
					user.phone(),
					user.barberId(),
					user.active(),
					Instant.parse("2026-06-18T12:00:00Z"),
					Instant.parse("2026-06-18T12:00:00Z"),
					user.roles()
			);
		});

		var response = service.create(command(Set.of(Role.BARBER), null, 5L));

		assertThat(response.barberId()).isEqualTo(5L);
		ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
		verify(userAccountRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(1L);
		assertThat(captor.getValue().branchId()).isEqualTo(1L);
		assertThat(captor.getValue().barberId()).isEqualTo(5L);
		assertThat(captor.getValue().passwordHash()).isEqualTo("$2a$hash");
	}

	private UserAccountService service(AuthenticatedUserResponse actor) {
		when(currentUserProvider.currentUser()).thenReturn(Optional.of(actor));
		return new UserAccountService(
				userAccountRepositoryPort,
				branchRepositoryPort,
				barberRepositoryPort,
				passwordHasherPort,
				new CurrentUserResolver(currentUserProvider),
				new UserAuthorizationPolicy()
		);
	}

	private CreateUserAccountCommand command(Set<Role> roles, Long branchId, Long barberId) {
		return new CreateUserAccountCommand(
				"barber@example.com",
				"StrongPassword123!",
				"Barber User",
				null,
				branchId,
				barberId,
				roles
		);
	}

	private AuthenticatedUserResponse companyOwner() {
		return new AuthenticatedUserResponse(1L, "owner@example.com", "Owner", 1L, 1L, null, Set.of(Role.COMPANY_OWNER));
	}

	private AuthenticatedUserResponse branchManager() {
		return new AuthenticatedUserResponse(2L, "manager@example.com", "Manager", 1L, 1L, null, Set.of(Role.BRANCH_MANAGER));
	}
}
