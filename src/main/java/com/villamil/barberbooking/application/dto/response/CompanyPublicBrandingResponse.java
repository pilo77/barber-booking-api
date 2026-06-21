package com.villamil.barberbooking.application.dto.response;

import com.villamil.barberbooking.domain.model.CompanyPublicProfile;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;

public record CompanyPublicBrandingResponse(
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

	public static CompanyPublicBrandingResponse from(CompanyPublicProfile profile) {
		return new CompanyPublicBrandingResponse(
				profile.publicName(),
				profile.publicDescription(),
				profile.logoUrl(),
				profile.coverImageUrl(),
				profile.primaryColor(),
				profile.secondaryColor(),
				profile.accentColor(),
				profile.themeMode(),
				profile.contactPhone(),
				profile.whatsappUrl(),
				profile.instagramUrl(),
				profile.facebookUrl(),
				profile.tiktokUrl(),
				profile.websiteUrl()
		);
	}
}
