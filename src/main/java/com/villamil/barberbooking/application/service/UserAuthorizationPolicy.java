package com.villamil.barberbooking.application.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.dto.response.AuthenticatedUserResponse;
import com.villamil.barberbooking.domain.exception.ForbiddenOperationException;
import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

@Component
class UserAuthorizationPolicy {

	private static final Set<Role> COMPANY_ASSIGNABLE_ROLES = Set.of(
			Role.BRANCH_MANAGER,
			Role.RECEPTIONIST,
			Role.BARBER,
			Role.CASHIER,
			Role.ACCOUNTANT,
			Role.INVENTORY_MANAGER
	);

	void ensureCanManageUsers(AuthenticatedUserResponse actor) {
		if (hasAnyRole(actor, Role.PLATFORM_OWNER, Role.COMPANY_OWNER, Role.BRANCH_MANAGER)) {
			return;
		}
		throw new ForbiddenOperationException("User role cannot manage user accounts");
	}

	void ensureCanAssignRoles(AuthenticatedUserResponse actor, Set<Role> roles) {
		if (actor.roles().contains(Role.PLATFORM_OWNER)) {
			return;
		}
		if (actor.roles().contains(Role.COMPANY_OWNER) && COMPANY_ASSIGNABLE_ROLES.containsAll(roles)) {
			return;
		}
		throw new ForbiddenOperationException("User role cannot assign requested roles");
	}

	void ensureCanAccessUser(AuthenticatedUserResponse actor, UserAccount target) {
		if (actor.roles().contains(Role.PLATFORM_OWNER)) {
			return;
		}
		if (actor.roles().contains(Role.COMPANY_OWNER) && actor.companyId().equals(target.companyId())) {
			return;
		}
		if (actor.roles().contains(Role.BRANCH_MANAGER)
				&& actor.companyId().equals(target.companyId())
				&& actor.branchId().equals(target.branchId())) {
			return;
		}
		throw new ForbiddenOperationException("User account is outside current scope");
	}

	private boolean hasAnyRole(AuthenticatedUserResponse actor, Role... roles) {
		for (Role role : roles) {
			if (actor.roles().contains(role)) {
				return true;
			}
		}
		return false;
	}
}
