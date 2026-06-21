package com.villamil.barberbooking.application.dto.command;

import com.villamil.barberbooking.domain.valueobject.ThemeMode;

public record UpdateCompanyPublicBrandingCommand(
		String publicName,
		String publicDescription,
		String logoUrl,
		String coverImageUrl,
		String primaryColor,
		String secondaryColor,
		String accentColor,
		ThemeMode themeMode,
		String contactPhone,
		String whatsappUrl,
		String instagramUrl,
		String facebookUrl,
		String tiktokUrl,
		String websiteUrl
) {
}
