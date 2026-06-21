package com.villamil.barberbooking.application.dto.response;

public record PublicBarberShopResponse(
		String slug,
		String name,
		String description,
		String logoUrl,
		boolean active,
		CompanyPublicBrandingResponse branding
) {
}
