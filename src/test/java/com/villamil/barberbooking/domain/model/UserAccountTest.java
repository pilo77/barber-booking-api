package com.villamil.barberbooking.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;

class UserAccountTest {

	@Test
	void barberIdRequiresAssignedTenant() {
		assertThatThrownBy(() -> new UserAccount(
				1L,
				null,
				null,
				"barber@example.com",
				"$2a$hash",
				"Barber User",
				null,
				5L,
				true,
				Instant.parse("2026-06-18T12:00:00Z"),
				Instant.parse("2026-06-18T12:00:00Z"),
				Set.of(Role.BARBER)
		))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Barber id requires assigned company and branch");
	}
}
