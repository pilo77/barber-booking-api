package com.villamil.barberbooking.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.villamil.barberbooking.application.dto.command.CreatePublicAppointmentCommand;

@Component
class PublicBookingRequestHasher {

	String hash(CreatePublicAppointmentCommand command, Long companyId, Long branchId) {
		String canonical = String.join("\n",
				String.valueOf(companyId),
				String.valueOf(branchId),
				String.valueOf(command.serviceOfferingId()),
				String.valueOf(command.barberId()),
				command.startAt().toString(),
				normalizeCaseInsensitive(command.customer().fullName()),
				normalize(command.customer().phone()),
				normalizeCaseInsensitive(command.customer().email())
		);
		try {
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))
			);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private String normalizeCaseInsensitive(String value) {
		return normalize(value).toLowerCase(Locale.ROOT);
	}

	private String normalize(String value) {
		return value == null ? "" : value.strip();
	}
}
