package com.villamil.barberbooking.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.villamil.barberbooking.application.dto.command.CreateUserAccountCommand;
import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.application.dto.response.UserAccountResponse;
import com.villamil.barberbooking.application.port.in.ActivateUserAccountUseCase;
import com.villamil.barberbooking.application.port.in.CreateUserAccountUseCase;
import com.villamil.barberbooking.application.port.in.DeactivateUserAccountUseCase;
import com.villamil.barberbooking.application.port.in.GetUserAccountUseCase;
import com.villamil.barberbooking.application.port.in.ListUserAccountsUseCase;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.UserAccountAlreadyExistsException;
import com.villamil.barberbooking.domain.exception.UserAccountNotFoundException;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

@Service
class UserAccountService implements
		CreateUserAccountUseCase,
		ListUserAccountsUseCase,
		GetUserAccountUseCase,
		ActivateUserAccountUseCase,
		DeactivateUserAccountUseCase {

	private final UserAccountRepositoryPort userAccountRepositoryPort;
	private final PasswordHasherPort passwordHasherPort;
	private final CurrentUserResolver currentUserResolver;
	private final UserAuthorizationPolicy userAuthorizationPolicy;

	UserAccountService(
			UserAccountRepositoryPort userAccountRepositoryPort,
			PasswordHasherPort passwordHasherPort,
			CurrentUserResolver currentUserResolver,
			UserAuthorizationPolicy userAuthorizationPolicy
	) {
		this.userAccountRepositoryPort = userAccountRepositoryPort;
		this.passwordHasherPort = passwordHasherPort;
		this.currentUserResolver = currentUserResolver;
		this.userAuthorizationPolicy = userAuthorizationPolicy;
	}

	@Override
	@Transactional
	public UserAccountResponse create(CreateUserAccountCommand command) {
		AuthenticatedUserResponse actor = currentUserResolver.requireCurrentUser();
		userAuthorizationPolicy.ensureCanManageUsers(actor);
		Set<Role> roles = validateRoles(command.roles());
		userAuthorizationPolicy.ensureCanAssignRoles(actor, roles);
		String email = normalizeEmail(command.email());
		if (userAccountRepositoryPort.existsByEmail(email)) {
			throw new UserAccountAlreadyExistsException("User account email already exists");
		}
		UserAccount userAccount = UserAccount.create(
				actor.companyId(),
				actor.branchId(),
				email,
				passwordHasherPort.hash(command.password()),
				command.fullName(),
				command.phone(),
				roles
		);
		return UserAccountResponse.from(userAccountRepositoryPort.save(userAccount));
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserAccountResponse> list() {
		AuthenticatedUserResponse actor = currentUserResolver.requireCurrentUser();
		userAuthorizationPolicy.ensureCanManageUsers(actor);
		if (actor.roles().contains(Role.PLATFORM_OWNER)) {
			return userAccountRepositoryPort.findAll().stream().map(UserAccountResponse::from).toList();
		}
		if (actor.roles().contains(Role.COMPANY_OWNER)) {
			return userAccountRepositoryPort.findAllByCompanyId(actor.companyId()).stream()
					.map(UserAccountResponse::from)
					.toList();
		}
		return userAccountRepositoryPort.findAllByCompanyIdAndBranchId(actor.companyId(), actor.branchId()).stream()
				.map(UserAccountResponse::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public UserAccountResponse getById(Long id) {
		AuthenticatedUserResponse actor = currentUserResolver.requireCurrentUser();
		UserAccount userAccount = userAccountRepositoryPort.findById(id)
				.orElseThrow(() -> new UserAccountNotFoundException("User account not found"));
		userAuthorizationPolicy.ensureCanAccessUser(actor, userAccount);
		return UserAccountResponse.from(userAccount);
	}

	@Override
	@Transactional
	public UserAccountResponse activate(Long id) {
		AuthenticatedUserResponse actor = currentUserResolver.requireCurrentUser();
		UserAccount userAccount = userAccountRepositoryPort.findById(id)
				.orElseThrow(() -> new UserAccountNotFoundException("User account not found"));
		userAuthorizationPolicy.ensureCanAccessUser(actor, userAccount);
		return UserAccountResponse.from(userAccountRepositoryPort.save(userAccount.activate()));
	}

	@Override
	@Transactional
	public UserAccountResponse deactivate(Long id) {
		AuthenticatedUserResponse actor = currentUserResolver.requireCurrentUser();
		UserAccount userAccount = userAccountRepositoryPort.findById(id)
				.orElseThrow(() -> new UserAccountNotFoundException("User account not found"));
		userAuthorizationPolicy.ensureCanAccessUser(actor, userAccount);
		return UserAccountResponse.from(userAccountRepositoryPort.save(userAccount.deactivate()));
	}

	private Set<Role> validateRoles(Set<Role> roles) {
		if (roles == null || roles.isEmpty()) {
			throw new BusinessRuleException("At least one role is required");
		}
		if (roles.contains(Role.PLATFORM_OWNER) || roles.contains(Role.COMPANY_OWNER) || roles.contains(Role.CUSTOMER)) {
			throw new BusinessRuleException("Requested role is not assignable from this endpoint");
		}
		return Set.copyOf(roles);
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new BusinessRuleException("Email is required");
		}
		return email.strip().toLowerCase();
	}
}
