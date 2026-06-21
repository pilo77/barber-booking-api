package com.villamil.barberbooking.application.dto.response;

public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		AuthenticatedUserResponse user
) {
}
