package com.villamil.barberbooking.application.dto.response;

import java.util.Set;

import com.villamil.barberbooking.domain.model.Role;
import com.villamil.barberbooking.domain.model.UserAccount;

public record AuthenticatedUserResponse(
		Long id,
		String email,
		String fullName,
		Long companyId,
		Long branchId,
		Set<Role> roles
) {

	public static AuthenticatedUserResponse from(UserAccount userAccount) {
		return new AuthenticatedUserResponse(
				userAccount.id(),
				userAccount.email(),
				userAccount.fullName(),
				userAccount.companyId(),
				userAccount.branchId(),
				userAccount.roles()
		);
	}
}
