package com.villamil.barberbooking.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.villamil.barberbooking.application.dto.command.CreatePublicAppointmentCommand;
import com.villamil.barberbooking.application.dto.command.PublicCustomerCommand;

class PublicBookingRequestHasherTest {

	private final PublicBookingRequestHasher hasher = new PublicBookingRequestHasher();

	@Test
	void equivalentNormalizedRequestsProduceSameHash() {
		CreatePublicAppointmentCommand first = command("Ponte-Perro", " Carlos Villamil ", "CLIENTE@EXAMPLE.COM");
		CreatePublicAppointmentCommand second = command("ponte-perro", "carlos villamil", "cliente@example.com");

		assertThat(hasher.hash(first)).isEqualTo(hasher.hash(second));
		assertThat(hasher.hash(first)).hasSize(64);
	}

	@Test
	void relevantRequestChangeProducesDifferentHash() {
		CreatePublicAppointmentCommand first = command("ponte-perro", "Carlos Villamil", "cliente@example.com");
		CreatePublicAppointmentCommand second = new CreatePublicAppointmentCommand(
				"ponte-perro", "neiva-centro", 1L, 3L,
				LocalDateTime.of(2026, 6, 21, 10, 0),
				new PublicCustomerCommand("Carlos Villamil", "3001234567", "cliente@example.com"),
				"same-key-123", "127.0.0.1"
		);

		assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(second));
	}

	private CreatePublicAppointmentCommand command(String companySlug, String fullName, String email) {
		return new CreatePublicAppointmentCommand(
				companySlug, "neiva-centro", 1L, 2L,
				LocalDateTime.of(2026, 6, 21, 10, 0),
				new PublicCustomerCommand(fullName, "3001234567", email),
				"same-key-123", "127.0.0.1"
		);
	}
}
