package com.villamil.barberbooking.application.dto.response;

public record PublicBarberResponse(
		Long id,
		String displayName,
		String photoUrl,
		String bio,
		String specialties
) {
}
