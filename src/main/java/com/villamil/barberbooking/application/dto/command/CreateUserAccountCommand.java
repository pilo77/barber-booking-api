package com.villamil.barberbooking.application.dto.command;

import java.util.Set;

import com.villamil.barberbooking.domain.model.Role;

public record CreateUserAccountCommand(
		String email,
		String password,
		String fullName,
		String phone,
		Long branchId,
		Long barberId,
		Set<Role> roles
) {
}
