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
import com.villamil.barberbooking.application.port.out.BarberRepositoryPort;
import com.villamil.barberbooking.application.port.out.BranchRepositoryPort;
import com.villamil.barberbooking.application.port.out.PasswordHasherPort;
import com.villamil.barberbooking.application.port.out.UserAccountRepositoryPort;
import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
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
	private final BranchRepositoryPort branchRepositoryPort;
	private final BarberRepositoryPort barberRepositoryPort;
	private final PasswordHasherPort passwordHasherPort;
	private final CurrentUserResolver currentUserResolver;
	private final UserAuthorizationPolicy userAuthorizationPolicy;

	UserAccountService(
			UserAccountRepositoryPort userAccountRepositoryPort,
			BranchRepositoryPort branchRepositoryPort,
			BarberRepositoryPort barberRepositoryPort,
			PasswordHasherPort passwordHasherPort,
			CurrentUserResolver currentUserResolver,
			UserAuthorizationPolicy userAuthorizationPolicy
	) {
		this.userAccountRepositoryPort = userAccountRepositoryPort;
		this.branchRepositoryPort = branchRepositoryPort;
		this.barberRepositoryPort = barberRepositoryPort;
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
		Long targetBranchId = resolveTargetBranch(actor, command.branchId());
		Long barberId = resolveBarberId(actor.companyId(), targetBranchId, roles, command.barberId());
		UserAccount userAccount = UserAccount.create(
				actor.companyId(),
				targetBranchId,
				email,
				passwordHasherPort.hash(command.password()),
				command.fullName(),
				command.phone(),
				barberId,
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

	private Long resolveTargetBranch(AuthenticatedUserResponse actor, Long requestedBranchId) {
		if (actor.companyId() == null || actor.branchId() == null) {
			throw new BusinessRuleException("User creation requires an assigned company and branch");
		}
		Long targetBranchId = requestedBranchId == null ? actor.branchId() : requestedBranchId;
		if (actor.roles().contains(Role.BRANCH_MANAGER) && !actor.branchId().equals(targetBranchId)) {
			throw new ForbiddenOperationException("Branch manager cannot create users outside current branch");
		}
		if (!branchRepositoryPort.existsByIdAndCompanyId(targetBranchId, actor.companyId())) {
			throw new BusinessRuleException("Branch does not belong to current company");
		}
		return targetBranchId;
	}

	private Long resolveBarberId(Long companyId, Long branchId, Set<Role> roles, Long barberId) {
		if (!roles.contains(Role.BARBER)) {
			if (barberId != null) {
				throw new BusinessRuleException("Barber id can only be assigned to BARBER users");
			}
			return null;
		}
		if (barberId == null) {
			throw new BusinessRuleException("BARBER users require barberId");
		}
		if (userAccountRepositoryPort.existsByBarberId(barberId)) {
			throw new BusinessRuleException("Barber is already linked to another user account");
		}
		if (!barberRepositoryPort.existsByIdAndCompanyIdAndBranchId(barberId, companyId, branchId)) {
			throw new BusinessRuleException("Barber does not belong to target company and branch");
		}
		return barberId;
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new BusinessRuleException("Email is required");
		}
		return email.strip().toLowerCase();
	}
}
