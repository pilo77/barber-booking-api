package com.villamil.barberbooking.application.dto.response;

import java.time.Instant;
import java.util.Set;

import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

public record UserAccountResponse(
		Long id,
		Long companyId,
		Long branchId,
		String email,
		String fullName,
		String phone,
		boolean active,
		Set<Role> roles,
		Instant createdAt,
		Instant updatedAt
) {

	public static UserAccountResponse from(UserAccount userAccount) {
		return new UserAccountResponse(
				userAccount.id(),
				userAccount.companyId(),
				userAccount.branchId(),
				userAccount.email(),
				userAccount.fullName(),
				userAccount.phone(),
				userAccount.active(),
				userAccount.roles(),
				userAccount.createdAt(),
				userAccount.updatedAt()
		);
	}
}
