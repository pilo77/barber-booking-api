package com.villamil.barberbooking.domain.model;

import java.time.Instant;
import java.util.Set;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

public record UserAccount(
		Long id,
		Long companyId,
		Long branchId,
		String email,
		String passwordHash,
		String fullName,
		String phone,
		boolean active,
		Instant createdAt,
		Instant updatedAt,
		Set<Role> roles
) {

	public UserAccount {
		validateId(id, "User account id");
		validateTenantPair(companyId, branchId);
		email = requireText(email, "User email is required").toLowerCase();
		passwordHash = requireText(passwordHash, "Password hash is required");
		fullName = requireText(fullName, "User full name is required");
		phone = normalizeOptionalText(phone);
		createdAt = createdAt == null ? Instant.now() : createdAt;
		updatedAt = updatedAt == null ? createdAt : updatedAt;
		if (roles == null || roles.isEmpty()) {
			throw new BusinessRuleException("At least one role is required");
		}
		roles = Set.copyOf(roles);
	}

	public static UserAccount create(
			Long companyId,
			Long branchId,
			String email,
			String passwordHash,
			String fullName,
			String phone,
			Set<Role> roles
	) {
		Instant now = Instant.now();
		return new UserAccount(null, companyId, branchId, email, passwordHash, fullName, phone, true, now, now, roles);
	}

	public UserAccount activate() {
		if (active) {
			return this;
		}
		return new UserAccount(id, companyId, branchId, email, passwordHash, fullName, phone, true, createdAt, Instant.now(), roles);
	}

	public UserAccount deactivate() {
		if (!active) {
			return this;
		}
		return new UserAccount(id, companyId, branchId, email, passwordHash, fullName, phone, false, createdAt, Instant.now(), roles);
	}

	public boolean hasRole(Role role) {
		return roles.contains(role);
	}

	private static void validateTenantPair(Long companyId, Long branchId) {
		if (companyId == null && branchId == null) {
			return;
		}
		if (companyId == null || companyId <= 0 || branchId == null || branchId <= 0) {
			throw new BusinessRuleException("Company id and branch id must be positive when assigned");
		}
	}

	private static String requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException(message);
		}
		return value.strip();
	}

	private static String normalizeOptionalText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}

	private static void validateId(Long value, String label) {
		if (value != null && value <= 0) {
			throw new BusinessRuleException(label + " must be positive");
		}
	}
}
