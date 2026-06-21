package com.villamil.barberbooking.domain.model;

import java.time.Instant;

import com.villamil.barberbooking.domain.exception.BusinessRuleException;
import com.villamil.barberbooking.domain.valueobject.ThemeMode;

public record CompanyPublicProfile(
		Long id,
		Long companyId,
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
		String websiteUrl,
		Instant createdAt,
		Instant updatedAt
) {

	public CompanyPublicProfile {
		if (id != null && id <= 0) {
			throw new BusinessRuleException("Company public profile id must be positive");
		}
		if (companyId == null || companyId <= 0) {
			throw new BusinessRuleException("Company id is required");
		}
		if (themeMode == null) {
			throw new BusinessRuleException("Theme mode is required");
		}
		createdAt = createdAt == null ? Instant.now() : createdAt;
		updatedAt = updatedAt == null ? createdAt : updatedAt;
	}

	public CompanyPublicProfile update(
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
		return new CompanyPublicProfile(
				id,
				companyId,
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
				websiteUrl,
				createdAt,
				Instant.now()
		);
	}
}
