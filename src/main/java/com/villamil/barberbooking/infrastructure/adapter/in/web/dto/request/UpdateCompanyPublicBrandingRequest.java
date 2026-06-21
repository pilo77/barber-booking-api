package com.villamil.barberbooking.infrastructure.adapter.in.web.dto.request;

import com.villamil.barberbooking.application.dto.command.UpdateCompanyPublicBrandingCommand;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;

import jakarta.validation.constraints.Size;

public record UpdateCompanyPublicBrandingRequest(
		@Size(max = 120, message = "Public name must be at most 120 characters")
		String publicName,

		@Size(max = 1000, message = "Public description must be at most 1000 characters")
		String publicDescription,

		@Size(max = 500, message = "Logo URL must be at most 500 characters")
		String logoUrl,

		@Size(max = 500, message = "Cover image URL must be at most 500 characters")
		String coverImageUrl,

		@Size(max = 7, message = "Primary color must be at most 7 characters")
		String primaryColor,

		@Size(max = 7, message = "Secondary color must be at most 7 characters")
		String secondaryColor,

		@Size(max = 7, message = "Accent color must be at most 7 characters")
		String accentColor,

		ThemeMode themeMode,

		@Size(max = 30, message = "Contact phone must be at most 30 characters")
		String contactPhone,

		@Size(max = 500, message = "WhatsApp URL must be at most 500 characters")
		String whatsappUrl,

		@Size(max = 500, message = "Instagram URL must be at most 500 characters")
		String instagramUrl,

		@Size(max = 500, message = "Facebook URL must be at most 500 characters")
		String facebookUrl,

		@Size(max = 500, message = "TikTok URL must be at most 500 characters")
		String tiktokUrl,

		@Size(max = 500, message = "Website URL must be at most 500 characters")
		String websiteUrl
) {

	public UpdateCompanyPublicBrandingCommand toCommand() {
		return new UpdateCompanyPublicBrandingCommand(
				publicName,
				publicDescription,
				logoUrl,
				coverImageUrl,
				primaryColor,
				secondaryColor,
				accentColor,
				themeMode,
				contactPhone,
				whatsappUrl,
				instagramUrl,
				facebookUrl,
				tiktokUrl,
				websiteUrl
		);
	}
}
